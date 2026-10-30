package io.github.hyinorbitcode.couponlab;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * ADR-0006: 불변식을 강제하는 DB 제약이 실제 MySQL에서 동작하는지 검증한다.
 * MySQL CHECK 위반(에러 3819)은 Spring이 DataIntegrityViolationException으로 변환하지 않으므로 제약 이름으로 검증한다.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
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

    private long insertDropEvent() {
        jdbcTemplate.update("""
                INSERT INTO drop_events (title, prize_description, open_at, close_at, status, created_at)
                VALUES ('커피 드롭', '커피 쿠폰', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6) + INTERVAL 1 HOUR,
                        'PUBLISHED', UTC_TIMESTAMP(6))
                """);
        return lastInsertId();
    }

    private long insertUser(String providerUserId) {
        jdbcTemplate.update("""
                INSERT INTO users (auth_provider, provider_user_id, nickname, role, created_at)
                VALUES ('EXPERIMENT', ?, ?, 'USER', UTC_TIMESTAMP(6))
                """, providerUserId, providerUserId);
        return lastInsertId();
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
