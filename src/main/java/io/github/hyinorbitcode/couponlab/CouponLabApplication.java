package io.github.hyinorbitcode.couponlab;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class CouponLabApplication {

    public static void main(String[] args) {
        SpringApplication.run(CouponLabApplication.class, args);
    }
}
