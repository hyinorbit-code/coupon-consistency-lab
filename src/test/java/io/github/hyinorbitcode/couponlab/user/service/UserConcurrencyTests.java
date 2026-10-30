package io.github.hyinorbitcode.couponlab.user.service;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.hyinorbitcode.couponlab.DatabaseCleaner;
import io.github.hyinorbitcode.couponlab.IntegrationTest;
import io.github.hyinorbitcode.couponlab.user.Role;
import io.github.hyinorbitcode.couponlab.user.entity.IdentityProvider;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

@Tag("concurrency")
@IntegrationTest
class UserConcurrencyTests {

    private static final int THREADS = 20;

    @Autowired
    UserService userService;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        DatabaseCleaner.truncateAll(jdbcTemplate);
    }

    @Test
    @DisplayName("같은 식별자로 동시에 첫 로그인해도 사용자는 한 명만 생기고 모두 같은 사용자를 받는다")
    void concurrentFirstLoginCreatesSingleUser() throws Exception {
        CountDownLatch ready = new CountDownLatch(THREADS);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Long>> results = new ArrayList<>();

        try (ExecutorService executor = Executors.newFixedThreadPool(THREADS)) {
            for (int i = 0; i < THREADS; i++) {
                results.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    return userService.findOrCreate(IdentityProvider.EXPERIMENT, "same-user", "닉네임", Role.USER).getId();
                }));
            }
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            Set<Long> userIds = new HashSet<>();
            for (Future<Long> result : results) {
                userIds.add(result.get(30, TimeUnit.SECONDS));
            }

            assertThat(userIds).hasSize(1);
        }
        // 막힌 쪽의 사용자 INSERT도 함께 롤백되어 연동 계정 없는 사용자가 남지 않는다.
        Integer users = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Integer.class);
        Integer identities = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM user_identities", Integer.class);
        assertThat(users).isEqualTo(1);
        assertThat(identities).isEqualTo(1);
    }
}
