package io.github.hyinorbitcode.couponlab;

import io.github.hyinorbitcode.couponlab.user.mail.VerificationMailSender;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 메일을 보내지 않고 메모리에 모은다. 통합 테스트에서 발송 여부와 링크를 검증할 때 쓴다.
 */
public class CapturingMailSender implements VerificationMailSender {

    private final List<SentMail> sent = new CopyOnWriteArrayList<>();

    @Override
    public void sendVerification(String to, String nickname, String verificationLink) {
        sent.add(new SentMail(MailType.VERIFICATION, to, verificationLink));
    }

    @Override
    public void sendAlreadyRegistered(String to, String loginLink) {
        sent.add(new SentMail(MailType.ALREADY_REGISTERED, to, loginLink));
    }

    public List<SentMail> sent() {
        return List.copyOf(sent);
    }

    public List<SentMail> sentOf(MailType type) {
        return sent.stream().filter(mail -> mail.type() == type).toList();
    }

    public void clear() {
        sent.clear();
    }

    public enum MailType {
        VERIFICATION,
        ALREADY_REGISTERED
    }

    public record SentMail(MailType type, String to, String link) {

        /** 인증 링크의 token 파라미터 값. */
        public String token() {
            return link.substring(link.indexOf("token=") + "token=".length());
        }
    }
}
