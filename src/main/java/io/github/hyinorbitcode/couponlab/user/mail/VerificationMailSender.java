package io.github.hyinorbitcode.couponlab.user.mail;

/**
 * 가입 관련 메일 발송. 실제 구현은 SMTP(SmtpVerificationMailSender), 테스트에서는 메모리에 모으는 가짜 구현을 쓴다.
 */
public interface VerificationMailSender {

    void sendVerification(String to, String nickname, String verificationLink);

    /** 이미 가입된 이메일로 다시 가입을 시도했을 때 보내는 안내. 응답으로는 가입 여부를 알려 주지 않는다. */
    void sendAlreadyRegistered(String to, String loginLink);
}
