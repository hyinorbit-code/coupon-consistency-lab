package io.github.hyinorbitcode.couponlab;

import org.springframework.boot.SpringApplication;

/**
 * 로컬 실행용 진입점. Testcontainers MySQL을 띄워 애플리케이션을 실행한다({@code ./gradlew bootTestRun}).
 */
public class TestCouponLabApplication {

    public static void main(String[] args) {
        SpringApplication.from(CouponLabApplication::main)
                .with(TestcontainersConfiguration.class)
                .run(args);
    }
}
