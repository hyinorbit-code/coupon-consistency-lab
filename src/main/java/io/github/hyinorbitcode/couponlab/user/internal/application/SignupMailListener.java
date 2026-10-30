package io.github.hyinorbitcode.couponlab.user.internal.application;

import io.github.hyinorbitcode.couponlab.user.internal.application.SignupMailEvents.AlreadyRegisteredMailRequested;
import io.github.hyinorbitcode.couponlab.user.internal.application.SignupMailEvents.VerificationMailRequested;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 가입 트랜잭션이 커밋된 뒤에만 메일을 보낸다. 롤백되면 메일도 나가지 않는다.
 * 발송이 실패해도 가입은 유지되고, 사용자는 재발송으로 복구한다(ADR-0012).
 */
@Slf4j
@Component
@RequiredArgsConstructor
class SignupMailListener {

    private final VerificationMailSender mailSender;
    private final WebProperties webProperties;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void on(VerificationMailRequested event) {
        String link = webProperties.baseUrl() + "/verify-email?token=" + event.rawToken();
        send(() -> mailSender.sendVerification(event.email(), event.nickname(), link));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void on(AlreadyRegisteredMailRequested event) {
        send(() -> mailSender.sendAlreadyRegistered(event.email(), webProperties.baseUrl() + "/login"));
    }

    private void send(Runnable sending) {
        try {
            sending.run();
        } catch (RuntimeException e) {
            log.warn("Failed to send signup mail; the user can request a resend", e);
        }
    }
}
