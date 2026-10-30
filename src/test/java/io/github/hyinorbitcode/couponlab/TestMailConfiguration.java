package io.github.hyinorbitcode.couponlab;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * 통합 테스트용 메일 발송기. spring.mail.host가 없으므로 SMTP 발송기는 등록되지 않고 이 가짜 발송기만 쓰인다.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestMailConfiguration {

    @Bean
    CapturingMailSender capturingMailSender() {
        return new CapturingMailSender();
    }
}
