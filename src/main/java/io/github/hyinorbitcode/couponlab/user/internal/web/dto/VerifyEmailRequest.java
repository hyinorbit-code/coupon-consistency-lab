package io.github.hyinorbitcode.couponlab.user.internal.web.dto;

import jakarta.validation.constraints.NotBlank;

public record VerifyEmailRequest(@NotBlank String token) {
}
