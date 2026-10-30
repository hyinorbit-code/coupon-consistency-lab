package io.github.hyinorbitcode.couponlab.user.internal.mail;

import io.github.hyinorbitcode.couponlab.user.internal.application.VerificationMailSender;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * SMTP 발송. spring.mail.host가 설정된 환경에서만 등록된다(로컬은 Mailpit, 테스트는 가짜 발송기).
 */
@Component
@ConditionalOnProperty("spring.mail.host")
@RequiredArgsConstructor
class SmtpVerificationMailSender implements VerificationMailSender {

    private final JavaMailSender javaMailSender;

    @Value("${couponlab.mail.from}")
    private String from;

    @Override
    public void sendVerification(String to, String nickname, String verificationLink) {
        send(to, "[Coupon Lab] 이메일 인증을 완료해 주세요", """
                %s님, 가입해 주셔서 감사합니다.

                아래 링크에서 이메일 인증을 완료해 주세요. 링크는 24시간 동안 한 번만 사용할 수 있습니다.

                %s

                본인이 가입하지 않았다면 이 메일을 무시하세요.
                """.formatted(nickname, verificationLink));
    }

    @Override
    public void sendAlreadyRegistered(String to, String loginLink) {
        send(to, "[Coupon Lab] 이미 가입된 이메일입니다", """
                이 이메일로 가입 요청이 들어왔지만, 이미 가입된 계정이 있습니다.

                로그인: %s

                본인이 요청하지 않았다면 이 메일을 무시하세요.
                """.formatted(loginLink));
    }

    private void send(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        javaMailSender.send(message);
    }
}
