package io.github.hyinorbitcode.couponlab;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * V1 시점에 존재하던 사용자 데이터가 V2 이후에도 같은 사용자 ID와 연동 계정으로 남는지 검증한다.
 * 다른 통합 테스트와 섞이지 않도록 전용 컨테이너에서 마이그레이션을 단계별로 실행한다.
 */
@Testcontainers
class MigrationV2Tests {

    @Container
    static final MySQLContainer MYSQL = new MySQLContainer(DockerImageName.parse("mysql:8.4"));

    @Test
    @DisplayName("V1의 사용자 제공자 정보가 V2에서 연동 계정으로 옮겨지고 활성 상태가 된다")
    void migratesExistingUsersToIdentities() {
        DataSource dataSource = new DriverManagerDataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        flyway(dataSource, "1").migrate();
        jdbcTemplate.update("""
                INSERT INTO users (auth_provider, provider_user_id, nickname, role, created_at)
                VALUES ('EXPERIMENT', 'legacy-user', '기존사용자', 'ADMIN', '2026-10-30 00:00:00.000000')
                """);
        long userId = jdbcTemplate.queryForObject("SELECT id FROM users", Long.class);

        flyway(dataSource, "latest").migrate();

        Map<String, Object> user = jdbcTemplate.queryForMap(
                "SELECT nickname, role, status, email, password_hash FROM users WHERE id = ?", userId);
        assertThat(user).containsEntry("nickname", "기존사용자")
                .containsEntry("role", "ADMIN")
                .containsEntry("status", "ACTIVE");
        assertThat(user.get("email")).isNull();
        assertThat(user.get("password_hash")).isNull();

        Map<String, Object> identity = jdbcTemplate.queryForMap(
                "SELECT provider, provider_user_id FROM user_identities WHERE user_id = ?", userId);
        assertThat(identity).containsEntry("provider", "EXPERIMENT")
                .containsEntry("provider_user_id", "legacy-user");

        Integer appliedVersions = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE success = 1", Integer.class);
        assertThat(appliedVersions).isEqualTo(2);
    }

    private static Flyway flyway(DataSource dataSource, String target) {
        return Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .target(target)
                .load();
    }
}
