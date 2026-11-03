package io.github.hyinorbitcode.couponlab;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class CouponLabApplicationTests {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("애플리케이션이 Testcontainers MySQL 8.4에 연결되어 기동된다")
    void contextLoadsWithMySql84(){
        String mySqlVersion =  jdbcTemplate.queryForObject("SELECT VERSION()",String.class);
        assertThat(mySqlVersion).startsWith("8.4");
    }
}
