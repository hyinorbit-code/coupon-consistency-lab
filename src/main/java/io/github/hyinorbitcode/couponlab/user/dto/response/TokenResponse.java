package io.github.hyinorbitcode.couponlab.user.dto.response;

import java.time.Instant;

public record TokenResponse(String accessToken, Instant expiresAt) {
}
