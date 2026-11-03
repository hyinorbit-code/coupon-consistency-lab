package io.github.hyinorbitcode.couponlab.user.dto.request;

import io.github.hyinorbitcode.couponlab.user.Role;
import io.github.hyinorbitcode.couponlab.user.entity.User;
import io.github.hyinorbitcode.couponlab.user.entity.UserIdentity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ExperimentTokenRequest(
        @NotBlank @Size(max = UserIdentity.MAX_PROVIDER_USER_ID_LENGTH) String providerUserId,
        @NotBlank @Size(max = User.MAX_NICKNAME_LENGTH) String nickname,
        @NotNull Role role
) {
}
