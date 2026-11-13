package io.github.hyinorbitcode.couponlab.drop.dto.response;

import io.github.hyinorbitcode.couponlab.drop.entity.DropPhase;
import io.github.hyinorbitcode.couponlab.drop.service.DropView;

/** 화면이 짧은 주기로 폴링하는 남은 수량. 성능 단계에서 캐시 대상이다. */
public record DropStockResponse(long remainingQuantity, DropPhase phase) {

    public static DropStockResponse from(DropView view) {
        return new DropStockResponse(view.remainingQuantity(), view.phase());
    }
}
