package io.github.hyinorbitcode.couponlab.user.controller;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.hyinorbitcode.couponlab.TestMailConfiguration;
import io.github.hyinorbitcode.couponlab.TestcontainersConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * experiment 프로필 없이 기동하면 실험용 토큰 발급 엔드포인트가 등록되지 않는다.
 */
@SpringBootTest
@Import({TestcontainersConfiguration.class, TestMailConfiguration.class})
@ActiveProfiles("test")
class ExperimentProfileDisabledTests {

    @Autowired
    ApplicationContext applicationContext;

    @Test
    @DisplayName("experiment 프로필이 없으면 토큰 발급 컨트롤러가 없다")
    void experimentEndpointIsAbsent() {
        assertThat(applicationContext.getBeanNamesForType(ExperimentAuthController.class)).isEmpty();
    }
}
