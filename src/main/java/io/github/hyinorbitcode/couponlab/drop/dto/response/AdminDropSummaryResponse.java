package io.github.hyinorbitcode.couponlab.drop.dto.response;

import io.github.hyinorbitcode.couponlab.drop.entity.DropEventStatus;
import io.github.hyinorbitcode.couponlab.drop.entity.DropPhase;
import io.github.hyinorbitcode.couponlab.drop.service.AdminDropView;

import java.time.Instant;

public record AdminDropSummaryResponse(
        Long id,
        String title,
        DropEventStatus status,
        DropPhase phase,
        Instant openAt,
        Instant closeAt,
        long registeredCodes,
        long assignedCodes
) {
    public static AdminDropSummaryResponse from(AdminDropView view) {
        return new AdminDropSummaryResponse(
                view.event().getId(), view.event().getTitle(), view.event().getStatus(), view.phase(),
                view.event().getOpenAt(), view.event().getCloseAt(),
                view.registeredCodes(), view.assignedCodes());
    }
}
