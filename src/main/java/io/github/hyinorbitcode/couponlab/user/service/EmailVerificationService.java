package io.github.hyinorbitcode.couponlab.user.service;

import io.github.hyinorbitcode.couponlab.user.config.SignupProperties;
import io.github.hyinorbitcode.couponlab.user.entity.EmailVerificationToken;
import io.github.hyinorbitcode.couponlab.user.entity.User;
import io.github.hyinorbitcode.couponlab.user.event.SignupMailEvents.VerificationMailRequested;
import io.github.hyinorbitcode.couponlab.user.exception.InvalidVerificationTokenException;
import io.github.hyinorbitcode.couponlab.user.repository.EmailVerificationTokenRepository;
import io.github.hyinorbitcode.couponlab.user.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final EmailVerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final VerificationTokens verificationTokens;
    private final SignupProperties signupProperties;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    /**
     * 토큰으로 이메일 인증을 마친다. 없거나 만료됐거나 이미 쓴 토큰이면 같은 오류를 낸다.
     * 사용 처리는 조건부 UPDATE로 해서 같은 토큰의 동시 요청 중 하나만 성공한다.
     */
    @Transactional
    public void verify(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new InvalidVerificationTokenException();
        }
        Instant now = clock.instant();
        EmailVerificationToken token = tokenRepository.findByTokenHash(verificationTokens.hash(rawToken))
                .orElseThrow(InvalidVerificationTokenException::new);
        if (token.isExpired(now) || tokenRepository.markUsedIfUnused(token.getId(), now) == 0) {
            throw new InvalidVerificationTokenException();
        }
        userRepository.findById(token.getUserId())
                .orElseThrow(InvalidVerificationTokenException::new)
                .verifyEmail(now);
    }

    /**
     * 인증 메일 재발송. 계정이 없거나 이미 인증됐거나 간격이 지나지 않았으면 아무 일도 하지 않는다.
     * 어느 경우든 호출자는 같은 응답을 받는다.
     */
    @Transactional
    public void resend(String email) {
        userRepository.findByEmail(User.normalizeEmail(email))
                .filter(User::isPendingVerification)
                .ifPresent(this::issueAndRequestMailIfIntervalPassed);
    }

    /** 호출자의 트랜잭션 안에서 토큰을 만들고, 커밋 후 발송될 메일을 요청한다. */
    public void issueAndRequestMail(User user) {
        var generated = verificationTokens.generate();
        tokenRepository.save(EmailVerificationToken.issue(
                user.getId(), generated.hash(), clock.instant(), signupProperties.verificationTtl()));
        eventPublisher.publishEvent(new VerificationMailRequested(user.getEmail(), user.getNickname(), generated.raw()));
    }

    public void issueAndRequestMailIfIntervalPassed(User user) {
        Instant now = clock.instant();
        boolean recentlySent = tokenRepository.findFirstByUserIdOrderByCreatedAtDesc(user.getId())
                .map(last -> last.getCreatedAt().plus(signupProperties.resendInterval()).isAfter(now))
                .orElse(false);
        if (!recentlySent) {
            issueAndRequestMail(user);
        }
    }
}
