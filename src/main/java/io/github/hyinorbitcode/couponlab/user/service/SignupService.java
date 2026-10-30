package io.github.hyinorbitcode.couponlab.user.service;

import io.github.hyinorbitcode.couponlab.user.entity.EmailAddress;
import io.github.hyinorbitcode.couponlab.user.entity.User;
import io.github.hyinorbitcode.couponlab.user.event.SignupMailEvents.AlreadyRegisteredMailRequested;
import io.github.hyinorbitcode.couponlab.user.repository.UserRepository;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 이메일 회원가입. 응답으로 계정 존재 여부를 드러내지 않는다(ADR-0012).
 * 새 이메일이면 인증 대기 사용자를 만들고, 이미 있으면 상태에 따라 인증 메일 재발송 또는 가입 안내 메일만 보낸다.
 */
@Service
@RequiredArgsConstructor
public class SignupService {

    private final UserRepository userRepository;
    private final EmailVerificationService emailVerificationService;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    public void signUp(String email, String rawPassword, String nickname) {
        PasswordPolicy.validate(rawPassword);
        EmailAddress emailAddress = new EmailAddress(email);
        // BCrypt는 일부러 느리므로 트랜잭션 밖에서 해시한다.
        String passwordHash = passwordEncoder.encode(rawPassword);
        try {
            transactionTemplate.executeWithoutResult(status -> userRepository.findByEmail(emailAddress)
                    .ifPresentOrElse(this::handleExisting,
                            () -> createPendingUser(emailAddress, passwordHash, nickname)));
        } catch (DataIntegrityViolationException concurrentSignup) {
            // 같은 이메일의 동시 가입에서 진 쪽. 먼저 커밋된 사용자를 기존 사용자로 처리한다.
            transactionTemplate.executeWithoutResult(status -> userRepository.findByEmail(emailAddress)
                    .ifPresent(this::handleExisting));
        }
    }

    private void createPendingUser(EmailAddress email, String passwordHash, String nickname) {
        User user = userRepository.saveAndFlush(
                User.signUpWithEmail(email, passwordHash, nickname, clock.instant()));
        emailVerificationService.issueAndRequestMail(user);
    }

    private void handleExisting(User user) {
        if (user.isPendingVerification()) {
            emailVerificationService.issueAndRequestMailIfIntervalPassed(user);
        } else {
            eventPublisher.publishEvent(new AlreadyRegisteredMailRequested(user.getEmail().value()));
        }
    }
}
