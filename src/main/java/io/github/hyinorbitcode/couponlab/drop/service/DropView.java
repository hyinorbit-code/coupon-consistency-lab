package io.github.hyinorbitcode.couponlab.drop.service;

import io.github.hyinorbitcode.couponlab.drop.entity.DropEvent;
import io.github.hyinorbitcode.couponlab.drop.entity.DropPhase;

/** 사용자에게 보여 줄 이벤트와 수량, 단계. 로그인 사용자의 당첨 경품 ID는 없으면 null. */
public record DropView(
        DropEvent event,
        long totalQuantity,
        long remainingQuantity,
        DropPhase phase,
        Long myPrizeId
) {
}
