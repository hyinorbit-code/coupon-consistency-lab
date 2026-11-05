package io.github.hyinorbitcode.couponlab.user.service;

import io.github.hyinorbitcode.couponlab.DatabaseCleaner;
import io.github.hyinorbitcode.couponlab.IntegrationTest;
import io.github.hyinorbitcode.couponlab.user.Role;
import io.github.hyinorbitcode.couponlab.user.entity.IdentityProvider;
import io.github.hyinorbitcode.couponlab.user.repository.UserIdentityRepository;
import io.github.hyinorbitcode.couponlab.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("concurrency")
@IntegrationTest
class UserConcurrencyTests {

    private static final int THREADS = 20;

    @Autowired
    UserService userService;

    @Autowired
    UserRepository userRepository;

    @Autowired
    UserIdentityRepository userIdentityRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        DatabaseCleaner.truncateAll(jdbcTemplate);
    }

    @Test
    @DisplayName("같은 외부 계정으로 동시에 첫 로그인해도 사용자는 한 명이고 모든 요청이 같은 사용자를 받는다")
    void concurrentFirstLoginCreatesSingleUser() throws Exception {
        CountDownLatch ready = new CountDownLatch(THREADS);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Long>> futures = new ArrayList<>();

        try (ExecutorService executor = Executors.newFixedThreadPool(THREADS)) {
            for (int i = 0; i < THREADS; i++) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    return userService.findOrCreate(IdentityProvider.EXPERIMENT, "same-user", "tester", Role.USER).getId();
                }));
            }
            ready.await();
            start.countDown();
        }

        List<Long> userIds = new ArrayList<>();
        List<Throwable> failures = new ArrayList<>();
        for (Future<Long> future : futures) {
            try {
                userIds.add(future.get());
            } catch (ExecutionException e) {
                failures.add(e.getCause());
            }
        }

        assertThat(failures).as("실패한 요청").isEmpty();
        assertThat(userIds).hasSize(THREADS).containsOnly(userIds.getFirst());
        assertThat(userRepository.count()).isEqualTo(1);
        assertThat(userIdentityRepository.count()).isEqualTo(1);
    }

}
