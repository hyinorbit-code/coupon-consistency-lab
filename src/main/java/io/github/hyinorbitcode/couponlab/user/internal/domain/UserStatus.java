package io.github.hyinorbitcode.couponlab.user.internal.domain;

public enum UserStatus {
    /** 이메일 가입 후 인증 전. 로그인할 수 없다. */
    PENDING_VERIFICATION,
    ACTIVE
}
