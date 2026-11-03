package io.github.hyinorbitcode.couponlab;

import org.springframework.boot.SpringApplication;

public class TestCouponLabApplication {

    public static void main(String[] args) {
        SpringApplication.from(CouponLabApplication::main)
                .with(TestcontainersConfiguration.class)
                .withAdditionalProfiles("test", "experiment")
                .run(args);
    }
    
}
