package io.github.hyinorbitcode.couponlab.user.internal.application;

import io.github.hyinorbitcode.couponlab.user.internal.application.SignupMailEvents.AlreadyRegisteredMailRequested;
import io.github.hyinorbitcode.couponlab.user.internal.domain.User;
import io.github.hyinorbitcode.couponlab.user.internal.domain.UserRepository;
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

    public static final int MIN_PASSWORD_LENGTH = 8;
    public static final int MAX_PASSWORD_LENGTH = 64;

    private final UserRepository userRepository;
    private final EmailVerificationService emailVerificationService;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    public void signUp(String email, String rawPassword, String nickname) {
        requirePasswordPolicy(rawPassword);
        String normalizedEmail = User.normalizeEmail(email);
        // BCrypt는 일부러 느리므로 트랜잭션 밖에서 해시한다.
        String passwordHash = passwordEncoder.encode(rawPassword);
        try {
            transactionTemplate.executeWithoutResult(status -> userRepository.findByEmail(normalizedEmail)
                    .ifPresentOrElse(this::handleExisting,
                            () -> createPendingUser(normalizedEmail, passwordHash, nickname)));
        } catch (DataIntegrityViolationException concurrentSignup) {
            // 같은 이메일의 동시 가입에서 진 쪽. 먼저 커밋된 사용자를 기존 사용자로 처리한다.
            transactionTemplate.executeWithoutResult(status -> userRepository.findByEmail(normalizedEmail)
                    .ifPresent(this::handleExisting));
        }
    }

    private void createPendingUser(String normalizedEmail, String passwordHash, String nickname) {
        User user = userRepository.saveAndFlush(
                User.signUpWithEmail(normalizedEmail, passwordHash, nickname, clock.instant()));
        emailVerificationService.issueAndRequestMail(user);
    }

    private void handleExisting(User user) {
        if (user.isPendingVerification()) {
            emailVerificationService.issueAndRequestMailIfIntervalPassed(user);
        } else {
            eventPublisher.publishEvent(new AlreadyRegisteredMailRequested(user.getEmail()));
        }
    }

    private static void requirePasswordPolicy(String rawPassword) {
        if (rawPassword == null || rawPassword.length() < MIN_PASSWORD_LENGTH
                || rawPassword.length() > MAX_PASSWORD_LENGTH || rawPassword.isBlank()) {
            throw new IllegalArgumentException(
                    "password must be " + MIN_PASSWORD_LENGTH + "-" + MAX_PASSWORD_LENGTH + " characters");
        }
    }
}
