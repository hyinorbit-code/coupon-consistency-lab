package io.github.hyinorbitcode.couponlab.user.internal.application;

import java.time.Duration;
import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param verificationTtl 인증 토큰 유효기간
 * @param resendInterval  같은 사용자에게 인증 메일을 다시 보낼 수 있는 최소 간격
 */
@ConfigurationProperties("couponlab.signup")
public record SignupProperties(Duration verificationTtl, Duration resendInterval) {

    public SignupProperties {
        Objects.requireNonNull(verificationTtl, "couponlab.signup.verification-ttl");
        Objects.requireNonNull(resendInterval, "couponlab.signup.resend-interval");
    }
}
