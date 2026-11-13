package io.github.hyinorbitcode.couponlab.drop.dto.response;

import io.github.hyinorbitcode.couponlab.drop.entity.DropPhase;
import io.github.hyinorbitcode.couponlab.drop.service.DropView;

import java.time.Instant;

/** 이벤트 상세. 로그인한 사용자가 이미 당첨됐으면 myPrizeId에 경품 ID가 들어간다. */
public record DropDetailResponse(
        Long id,
        String title,
        String prizeDescription,
        Instant openAt,
        Instant closeAt,
        long totalQuantity,
        long remainingQuantity,
        DropPhase phase,
        Long myPrizeId
) {
    public static DropDetailResponse from(DropView view) {
        return new DropDetailResponse(
                view.event().getId(), view.event().getTitle(), view.event().getPrizeDescription(),
                view.event().getOpenAt(), view.event().getCloseAt(),
                view.totalQuantity(), view.remainingQuantity(), view.phase(), view.myPrizeId());
    }
}
