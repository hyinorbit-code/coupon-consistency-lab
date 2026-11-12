package io.github.hyinorbitcode.couponlab.participation.dto.response;

import io.github.hyinorbitcode.couponlab.participation.service.EntryResult;

public record EntryResponse(String result, PrizeResponse prize) {

    public static final String WON = "WON";

    public static EntryResponse won(EntryResult entryResult) {
        return new EntryResponse(WON, PrizeResponse.from(entryResult));
    }
}
