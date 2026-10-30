package io.github.hyinorbitcode.couponlab.user.internal.web.dto;

import java.time.Instant;

public record TokenResponse(String accessToken, Instant expiresAt) {
}
