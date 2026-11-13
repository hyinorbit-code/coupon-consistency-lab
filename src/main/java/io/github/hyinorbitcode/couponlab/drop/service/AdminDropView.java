package io.github.hyinorbitcode.couponlab.drop.service;

import io.github.hyinorbitcode.couponlab.drop.entity.DropEvent;
import io.github.hyinorbitcode.couponlab.drop.entity.DropPhase;

import java.time.Instant;
import java.util.List;

/** 관리자에게 보여 줄 이벤트. 당첨자 목록은 상세에서만 채운다. */
public record AdminDropView(
        DropEvent event,
        DropPhase phase,
        long registeredCodes,
        long assignedCodes,
        long entryAttempts,
        List<Winner> winners
) {
    public record Winner(String nickname, String codeHint, Instant assignedAt) {
    }
}
