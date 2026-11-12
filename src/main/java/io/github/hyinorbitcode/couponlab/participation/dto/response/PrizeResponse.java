package io.github.hyinorbitcode.couponlab.participation.dto.response;

import io.github.hyinorbitcode.couponlab.participation.service.EntryResult;

import java.time.Instant;

public record PrizeResponse(
        long id,
        long dropId,
        String dropTitle,
        String prizeDescription,
        String code,
        Instant wonAt
) {
    public static PrizeResponse from(EntryResult result) {
        return new PrizeResponse(
                result.prize().prizeCodeId(),
                result.drop().id(),
                result.drop().title(),
                result.drop().prizeDescription(),
                result.prize().code(),
                result.prize().wonAt());
    }
}
