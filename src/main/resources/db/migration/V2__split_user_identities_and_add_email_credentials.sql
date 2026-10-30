-- ADR-0010: 사용자와 외부 연동 계정을 분리하고 이메일 로그인 정보를 추가한다.
-- MySQL DDL은 트랜잭션으로 묶이지 않으므로, 단계마다 독립적으로 실행돼도 되도록 순서를 둔다.

-- 1. 연동 계정 테이블. 카카오 회원번호 하나는 사용자 한 명에게만, 사용자당 제공자 하나.
CREATE TABLE user_identities (
    id               BIGINT      NOT NULL AUTO_INCREMENT,
    user_id          BIGINT      NOT NULL,
    provider         VARCHAR(20) NOT NULL,
    provider_user_id VARCHAR(64) NOT NULL,
    linked_at        DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_identities_provider (provider, provider_user_id),
    UNIQUE KEY uk_user_identities_user_provider (user_id, provider),
    CONSTRAINT fk_user_identities_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT ck_user_identities_provider CHECK (provider IN ('KAKAO', 'EXPERIMENT'))
);

-- 2. 기존 사용자의 제공자 정보를 연동 계정으로 옮긴다. 사용자 ID는 그대로 유지된다.
INSERT INTO user_identities (user_id, provider, provider_user_id, linked_at)
SELECT id, auth_provider, provider_user_id, created_at
FROM users;

-- 3. users에서 제공자 컬럼을 제거한다. CHECK가 참조하는 컬럼은 CHECK를 먼저 지워야 삭제할 수 있다.
ALTER TABLE users DROP CHECK ck_users_auth_provider;
ALTER TABLE users DROP INDEX uk_users_provider;
ALTER TABLE users
    DROP COLUMN auth_provider,
    DROP COLUMN provider_user_id;

-- 4. 이메일 로그인 정보와 상태. 기존 사용자는 모두 연동 계정 사용자이므로 ACTIVE로 채운다.
ALTER TABLE users
    ADD COLUMN email             VARCHAR(254) NULL AFTER id,
    ADD COLUMN password_hash     VARCHAR(100) NULL AFTER email,
    ADD COLUMN status            VARCHAR(30)  NOT NULL DEFAULT 'ACTIVE' AFTER role,
    ADD COLUMN email_verified_at DATETIME(6)  NULL AFTER status;
ALTER TABLE users ALTER COLUMN status DROP DEFAULT;

ALTER TABLE users
    ADD CONSTRAINT uk_users_email UNIQUE (email),
    ADD CONSTRAINT ck_users_status CHECK (status IN ('PENDING_VERIFICATION', 'ACTIVE')),
    -- 이메일과 비밀번호는 함께 있거나 함께 없다(카카오로만 가입한 사용자는 둘 다 없음).
    ADD CONSTRAINT ck_users_credentials CHECK ((email IS NULL) = (password_hash IS NULL)),
    -- 인증 대기는 이메일 가입자만, 아직 인증 전이어야 한다.
    ADD CONSTRAINT ck_users_pending CHECK (
        status <> 'PENDING_VERIFICATION' OR (email IS NOT NULL AND email_verified_at IS NULL)),
    -- 활성 이메일 가입자는 인증 시각이 있어야 한다.
    ADD CONSTRAINT ck_users_verified CHECK (
        email IS NULL OR status = 'PENDING_VERIFICATION' OR email_verified_at IS NOT NULL);

-- 5. 이메일 인증 토큰. 토큰 원문은 저장하지 않고 SHA-256 해시만 저장한다.
CREATE TABLE email_verification_tokens (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    user_id    BIGINT      NOT NULL,
    token_hash BINARY(32)  NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    used_at    DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_email_verification_tokens_hash (token_hash),
    KEY ix_email_verification_tokens_user (user_id, created_at),
    CONSTRAINT fk_email_verification_tokens_user FOREIGN KEY (user_id) REFERENCES users (id)
);
