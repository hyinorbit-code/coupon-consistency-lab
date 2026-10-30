package io.github.hyinorbitcode.couponlab.user.internal.web.dto;

import io.github.hyinorbitcode.couponlab.user.internal.application.SignupService;
import io.github.hyinorbitcode.couponlab.user.internal.domain.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @NotBlank @Email @Size(max = User.MAX_EMAIL_LENGTH) String email,
        @NotNull @Size(min = SignupService.MIN_PASSWORD_LENGTH, max = SignupService.MAX_PASSWORD_LENGTH)
        String password,
        @NotBlank @Size(max = User.MAX_NICKNAME_LENGTH) String nickname) {

    /** 앞뒤 공백은 검증 전에 지운다. 대소문자 정규화는 서비스에서 한다. */
    public SignupRequest {
        email = email == null ? null : email.strip();
    }
}
