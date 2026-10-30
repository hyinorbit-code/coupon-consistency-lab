package io.github.hyinorbitcode.couponlab;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 테스트 간 격리를 위해 Flyway 이력을 제외한 모든 테이블을 비운다.
 * 동시성 테스트는 실제 커밋이 필요하므로 트랜잭션 롤백 대신 이 방식을 쓴다(ADR-0004).
 */
public final class DatabaseCleaner {

    private DatabaseCleaner() {
    }

    public static void truncateAll(JdbcTemplate jdbcTemplate) {
        List<String> tables = jdbcTemplate.queryForList("""
                SELECT table_name
                FROM information_schema.tables
                WHERE table_schema = DATABASE()
                  AND table_type = 'BASE TABLE'
                  AND table_name <> 'flyway_schema_history'
                """, String.class);

        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0");
        try {
            tables.forEach(table -> jdbcTemplate.execute("TRUNCATE TABLE `" + table + "`"));
        } finally {
            jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1");
        }
    }
}
