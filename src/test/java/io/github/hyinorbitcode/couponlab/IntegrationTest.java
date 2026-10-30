package io.github.hyinorbitcode.couponlab;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Testcontainers MySQL과 테스트·실험 프로필로 전체 컨텍스트를 띄우는 통합 테스트.
 * 모든 통합 테스트가 같은 설정을 써서 스프링 컨텍스트와 컨테이너를 재사용한다(ADR-0004).
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, TestMailConfiguration.class})
@ActiveProfiles({"test", "experiment"})
public @interface IntegrationTest {
}
