package io.github.hyinorbitcode.couponlab.user.dto.request;

import io.github.hyinorbitcode.couponlab.user.entity.User;
import io.github.hyinorbitcode.couponlab.user.service.PasswordPolicy;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @NotBlank @Email @Size(max = User.MAX_EMAIL_LENGTH) String email,
        @NotNull @Size(min = PasswordPolicy.MIN_LENGTH, max = PasswordPolicy.MAX_LENGTH)
        String password,
        @NotBlank @Size(max = User.MAX_NICKNAME_LENGTH) String nickname) {

    /** 앞뒤 공백은 검증 전에 지운다. 대소문자 정규화는 서비스에서 한다. */
    public SignupRequest {
        email = email == null ? null : email.strip();
    }
}
