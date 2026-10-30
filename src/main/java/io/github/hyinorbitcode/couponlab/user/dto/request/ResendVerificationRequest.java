package io.github.hyinorbitcode.couponlab.user.dto.request;

import io.github.hyinorbitcode.couponlab.user.entity.EmailAddress;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResendVerificationRequest(@NotBlank @Email @Size(max = EmailAddress.MAX_LENGTH) String email) {

    /** 앞뒤 공백은 검증 전에 지운다. */
    public ResendVerificationRequest {
        email = email == null ? null : email.strip();
    }
}
