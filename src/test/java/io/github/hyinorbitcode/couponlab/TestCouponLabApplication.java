package io.github.hyinorbitcode.couponlab;

import org.springframework.boot.SpringApplication;

/**
 * 로컬 실행용 진입점. Testcontainers MySQL을 띄우고 test·experiment 프로필로 실행한다({@code ./gradlew bootTestRun}).
 */
public class TestCouponLabApplication {

    public static void main(String[] args) {
        SpringApplication.from(CouponLabApplication::main)
                .with(TestcontainersConfiguration.class)
                .withAdditionalProfiles("test", "experiment")
                .run(args);
    }
}
