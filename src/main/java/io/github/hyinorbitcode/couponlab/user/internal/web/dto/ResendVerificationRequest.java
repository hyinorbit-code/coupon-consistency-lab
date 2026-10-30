package io.github.hyinorbitcode.couponlab.user.internal.web.dto;

import io.github.hyinorbitcode.couponlab.user.internal.domain.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResendVerificationRequest(@NotBlank @Email @Size(max = User.MAX_EMAIL_LENGTH) String email) {

    /** 앞뒤 공백은 검증 전에 지운다. */
    public ResendVerificationRequest {
        email = email == null ? null : email.strip();
    }
}
