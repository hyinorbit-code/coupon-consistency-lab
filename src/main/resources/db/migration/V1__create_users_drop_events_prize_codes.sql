-- ADR-0006: 1단계 스키마. 모든 시각은 UTC로 저장한다.

CREATE TABLE users (
    id               BIGINT      NOT NULL AUTO_INCREMENT,
    auth_provider    VARCHAR(20) NOT NULL,
    provider_user_id VARCHAR(64) NOT NULL,
    nickname         VARCHAR(50) NOT NULL,
    role             VARCHAR(20) NOT NULL,
    created_at       DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_provider (auth_provider, provider_user_id),
    CONSTRAINT ck_users_auth_provider CHECK (auth_provider IN ('KAKAO', 'EXPERIMENT')),
    CONSTRAINT ck_users_role CHECK (role IN ('ADMIN', 'USER'))
);

CREATE TABLE drop_events (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    title             VARCHAR(100) NOT NULL,
    prize_description VARCHAR(500) NOT NULL,
    open_at           DATETIME(6)  NOT NULL,
    close_at          DATETIME(6)  NOT NULL,
    status            VARCHAR(20)  NOT NULL,
    created_at        DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT ck_drop_events_period CHECK (open_at < close_at),
    CONSTRAINT ck_drop_events_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'CANCELED'))
);

CREATE TABLE prize_codes (
    id               BIGINT         NOT NULL AUTO_INCREMENT,
    drop_event_id    BIGINT         NOT NULL,
    code_ciphertext  VARBINARY(512) NOT NULL,
    code_hash        BINARY(32)     NOT NULL,
    code_hint        VARCHAR(8)     NOT NULL,
    assigned_user_id BIGINT         NULL,
    assigned_at      DATETIME(6)    NULL,
    created_at       DATETIME(6)    NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_prize_codes_hash (code_hash),
    UNIQUE KEY uk_prize_codes_winner (drop_event_id, assigned_user_id),
    KEY ix_prize_codes_available (drop_event_id, assigned_user_id, id),
    CONSTRAINT fk_prize_codes_drop_event FOREIGN KEY (drop_event_id) REFERENCES drop_events (id),
    CONSTRAINT fk_prize_codes_assigned_user FOREIGN KEY (assigned_user_id) REFERENCES users (id),
    CONSTRAINT ck_prize_codes_assignment CHECK ((assigned_user_id IS NULL) = (assigned_at IS NULL))
);
