package io.github.hyinorbitcode.couponlab.drop.dto.response;

import io.github.hyinorbitcode.couponlab.drop.entity.DropEvent;
import io.github.hyinorbitcode.couponlab.drop.entity.DropEventStatus;
import io.github.hyinorbitcode.couponlab.drop.entity.DropPhase;

import java.time.Instant;

public record AdminDropResponse(
        Long id,
        String title,
        String prizeDescription,
        Instant openAt,
        Instant closeAt,
        DropEventStatus status,
        DropPhase phase,
        Instant createdAt
) {
    public static AdminDropResponse from(DropEvent event, Instant now) {
        return new AdminDropResponse(
                event.getId(), event.getTitle(), event.getPrizeDescription(),
                event.getOpenAt(), event.getCloseAt(), event.getStatus(),
                event.phase(now), event.getCreatedAt());
    }
}