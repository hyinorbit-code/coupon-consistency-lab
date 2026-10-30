package io.github.hyinorbitcode.couponlab;

import org.springframework.boot.SpringApplication;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * 로컬 실행용 진입점({@code ./gradlew bootTestRun}). Testcontainers로 MySQL과 Mailpit을 띄우고 test·experiment 프로필로 실행한다.
 * 발송된 메일은 콘솔에 출력되는 Mailpit 웹 화면 주소에서 확인한다.
 */
public class TestCouponLabApplication {

    private static final int MAILPIT_SMTP_PORT = 1025;
    private static final int MAILPIT_WEB_PORT = 8025;

    public static void main(String[] args) {
        GenericContainer<?> mailpit = new GenericContainer<>(DockerImageName.parse("axllent/mailpit:v1.32"))
                .withExposedPorts(MAILPIT_SMTP_PORT, MAILPIT_WEB_PORT);
        mailpit.start();
        System.setProperty("spring.mail.host", mailpit.getHost());
        System.setProperty("spring.mail.port", String.valueOf(mailpit.getMappedPort(MAILPIT_SMTP_PORT)));
        System.out.printf("%nMailpit 웹 화면: http://%s:%d%n%n", mailpit.getHost(), mailpit.getMappedPort(MAILPIT_WEB_PORT));

        SpringApplication.from(CouponLabApplication::main)
                .with(TestcontainersConfiguration.class)
                .withAdditionalProfiles("test", "experiment")
                .run(args);
    }
}
