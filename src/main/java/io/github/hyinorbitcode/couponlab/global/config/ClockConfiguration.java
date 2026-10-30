package io.github.hyinorbitcode.couponlab.global.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 시각이 필요한 곳은 이 Clock을 주입받아 쓴다. 테스트에서 고정 시각으로 바꿀 수 있다(ADR-0004).
 */
@Configuration(proxyBeanMethods = false)
public class ClockConfiguration {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
