package io.github.hyinorbitcode.couponlab.user.internal.application;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.hyinorbitcode.couponlab.CapturingMailSender;
import io.github.hyinorbitcode.couponlab.CapturingMailSender.MailType;
import io.github.hyinorbitcode.couponlab.DatabaseCleaner;
import io.github.hyinorbitcode.couponlab.IntegrationTest;
import java.util.ArrayList;
import java.util.List;
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
class SignupConcurrencyTests {

    private static final int THREADS = 20;

    @Autowired
    SignupService signupService;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    CapturingMailSender mailSender;

    @BeforeEach
    void setUp() {
        DatabaseCleaner.truncateAll(jdbcTemplate);
        mailSender.clear();
    }

    @Test
    @DisplayName("같은 이메일로 동시에 가입해도 사용자는 한 명이고 모든 요청이 오류 없이 끝난다")
    void concurrentSignupCreatesSingleUser() throws Exception {
        CountDownLatch ready = new CountDownLatch(THREADS);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> results = new ArrayList<>();

        try (ExecutorService executor = Executors.newFixedThreadPool(THREADS)) {
            for (int i = 0; i < THREADS; i++) {
                results.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    signupService.signUp("same@example.com", "correct-horse-battery", "가입자");
                    return null;
                }));
            }
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            for (Future<?> result : results) {
                result.get(60, TimeUnit.SECONDS);
            }
        }

        Integer users = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Integer.class);
        assertThat(users).isEqualTo(1);
        // 처음 만든 요청의 인증 메일은 반드시 나가고, 나머지는 재발송 간격 안이라 추가로 보내지 않는다.
        assertThat(mailSender.sentOf(MailType.VERIFICATION)).isNotEmpty();
    }
}
