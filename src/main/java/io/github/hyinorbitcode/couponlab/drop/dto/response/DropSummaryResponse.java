package io.github.hyinorbitcode.couponlab.drop.dto.response;

import io.github.hyinorbitcode.couponlab.drop.entity.DropPhase;
import io.github.hyinorbitcode.couponlab.drop.service.DropView;

import java.time.Instant;

public record DropSummaryResponse(
        Long id,
        String title,
        String prizeDescription,
        Instant openAt,
        Instant closeAt,
        long totalQuantity,
        long remainingQuantity,
        DropPhase phase
) {
    public static DropSummaryResponse from(DropView view) {
        return new DropSummaryResponse(
                view.event().getId(), view.event().getTitle(), view.event().getPrizeDescription(),
                view.event().getOpenAt(), view.event().getCloseAt(),
                view.totalQuantity(), view.remainingQuantity(), view.phase());
    }
}
