package io.github.hyinorbitcode.couponlab.prizecode;

import java.time.Instant;

public record AssignedPrize(long prizeCodeId, long dropEventId, String code, Instant wonAt) {
}
