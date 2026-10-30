package io.github.hyinorbitcode.couponlab;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * ADR-0006: 불변식을 강제하는 DB 제약이 실제 MySQL에서 동작하는지 검증한다.
 * MySQL CHECK 위반(에러 3819)은 Spring이 DataIntegrityViolationException으로 변환하지 않으므로 제약 이름으로 검증한다.
 */
@IntegrationTest
class SchemaConstraintTests {

    @Autowired
    JdbcTemplate jdbcTemplate;

    long dropEventId;
    long userId;
    long otherUserId;

    @BeforeEach
    void setUp() {
        DatabaseCleaner.truncateAll(jdbcTemplate);
        dropEventId = insertDropEvent();
        userId = insertUser("user-1");
        otherUserId = insertUser("user-2");
    }

    @Test
    @DisplayName("같은 이벤트에서 한 사용자에게 코드를 두 개 할당할 수 없다")
    void rejectsSecondCodeForSameUser() {
        long first = insertPrizeCode(1);
        long second = insertPrizeCode(2);
        assign(first, userId);

        assertThatThrownBy(() -> assign(second, userId))
                .isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    @DisplayName("미할당 코드는 같은 이벤트에 여러 개 존재할 수 있다")
    void allowsManyUnassignedCodes() {
        insertPrizeCode(1);
        insertPrizeCode(2);
        insertPrizeCode(3);

        Integer unassigned = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM prize_codes WHERE drop_event_id = ? AND assigned_user_id IS NULL",
                Integer.class, dropEventId);
        assertThat(unassigned).isEqualTo(3);
    }

    @Test
    @DisplayName("서로 다른 사용자는 같은 이벤트에서 각각 코드를 할당받을 수 있다")
    void allowsDifferentUsersInSameEvent() {
        assign(insertPrizeCode(1), userId);
        assign(insertPrizeCode(2), otherUserId);

        Integer assigned = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM prize_codes WHERE assigned_user_id IS NOT NULL", Integer.class);
        assertThat(assigned).isEqualTo(2);
    }

    @Test
    @DisplayName("할당 사용자와 할당 시각은 항상 함께 채워진다")
    void rejectsAssignmentWithoutTimestamp() {
        long codeId = insertPrizeCode(1);

        assertThatThrownBy(() -> jdbcTemplate.update(
                "UPDATE prize_codes SET assigned_user_id = ? WHERE id = ?", userId, codeId))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("ck_prize_codes_assignment");
    }

    @Test
    @DisplayName("같은 코드는 두 번 등록할 수 없다")
    void rejectsDuplicateCodeHash() {
        insertPrizeCode(1);

        assertThatThrownBy(() -> insertPrizeCode(1))
                .isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    @DisplayName("이벤트 종료 시각은 오픈 시각보다 늦어야 한다")
    void rejectsInvalidPeriod() {
        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO drop_events (title, prize_description, open_at, close_at, status, created_at)
                VALUES ('t', 'p', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6), 'DRAFT', UTC_TIMESTAMP(6))
                """))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("ck_drop_events_period");
    }

    @Test
    @DisplayName("카카오 회원번호 하나는 사용자 한 명에게만 연동된다")
    void rejectsSameKakaoIdentityForTwoUsers() {
        insertIdentity(userId, "KAKAO", "kakao-1");

        assertThatThrownBy(() -> insertIdentity(otherUserId, "KAKAO", "kakao-1"))
                .isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    @DisplayName("사용자 한 명에게 카카오 계정을 두 개 연동할 수 없다")
    void rejectsSecondKakaoIdentityForSameUser() {
        insertIdentity(userId, "KAKAO", "kakao-1");

        assertThatThrownBy(() -> insertIdentity(userId, "KAKAO", "kakao-2"))
                .isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    @DisplayName("같은 이메일로 두 사용자를 만들 수 없다")
    void rejectsDuplicateEmail() {
        insertEmailUser("user@example.com", "ACTIVE", true);

        assertThatThrownBy(() -> insertEmailUser("user@example.com", "PENDING_VERIFICATION", false))
                .isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    @DisplayName("이메일과 비밀번호 해시는 함께 있거나 함께 없다")
    void rejectsEmailWithoutPassword() {
        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO users (email, nickname, role, status, email_verified_at, created_at)
                VALUES ('user@example.com', 'nick', 'USER', 'ACTIVE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6))
                """))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("ck_users_credentials");
    }

    @Test
    @DisplayName("인증 대기 상태는 이메일 가입자만 가질 수 있다")
    void rejectsPendingUserWithoutEmail() {
        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO users (nickname, role, status, created_at)
                VALUES ('nick', 'USER', 'PENDING_VERIFICATION', UTC_TIMESTAMP(6))
                """))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("ck_users_pending");
    }

    @Test
    @DisplayName("활성 이메일 가입자는 인증 시각이 있어야 한다")
    void rejectsActiveEmailUserWithoutVerification() {
        assertThatThrownBy(() -> insertEmailUser("user@example.com", "ACTIVE", false))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("ck_users_verified");
    }

    private long insertDropEvent() {
        jdbcTemplate.update("""
                INSERT INTO drop_events (title, prize_description, open_at, close_at, status, created_at)
                VALUES ('커피 드롭', '커피 쿠폰', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6) + INTERVAL 1 HOUR,
                        'PUBLISHED', UTC_TIMESTAMP(6))
                """);
        return lastInsertId();
    }

    private long insertUser(String nickname) {
        jdbcTemplate.update("""
                INSERT INTO users (nickname, role, status, created_at)
                VALUES (?, 'USER', 'ACTIVE', UTC_TIMESTAMP(6))
                """, nickname);
        return lastInsertId();
    }

    private void insertIdentity(long ownerId, String provider, String providerUserId) {
        jdbcTemplate.update("""
                INSERT INTO user_identities (user_id, provider, provider_user_id, linked_at)
                VALUES (?, ?, ?, UTC_TIMESTAMP(6))
                """, ownerId, provider, providerUserId);
    }

    private void insertEmailUser(String email, String status, boolean verified) {
        jdbcTemplate.update("""
                INSERT INTO users (email, password_hash, nickname, role, status, email_verified_at, created_at)
                VALUES (?, '{bcrypt}hash', 'nick', 'USER', ?, IF(?, UTC_TIMESTAMP(6), NULL), UTC_TIMESTAMP(6))
                """, email, status, verified);
    }

    /** 실제 암호화는 1단계 기능 구현에서 다룬다. 여기서는 seed로 서로 다른 해시만 만든다. */
    private long insertPrizeCode(int seed) {
        jdbcTemplate.update("""
                INSERT INTO prize_codes (drop_event_id, code_ciphertext, code_hash, code_hint, created_at)
                VALUES (?, RANDOM_BYTES(32), UNHEX(SHA2(?, 256)), ?, UTC_TIMESTAMP(6))
                """, dropEventId, "code-" + seed, String.format("%04d", seed));
        return lastInsertId();
    }

    private void assign(long codeId, long assigneeId) {
        jdbcTemplate.update(
                "UPDATE prize_codes SET assigned_user_id = ?, assigned_at = UTC_TIMESTAMP(6) WHERE id = ?",
                assigneeId, codeId);
    }

    private long lastInsertId() {
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }
}
