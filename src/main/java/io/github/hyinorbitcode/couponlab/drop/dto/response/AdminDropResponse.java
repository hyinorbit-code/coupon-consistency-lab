package io.github.hyinorbitcode.couponlab.drop.dto.response;

import io.github.hyinorbitcode.couponlab.drop.entity.DropEventStatus;
import io.github.hyinorbitcode.couponlab.drop.entity.DropPhase;
import io.github.hyinorbitcode.couponlab.drop.service.AdminDropView;

import java.time.Instant;
import java.util.List;

/** 관리자용 이벤트 상세. 당첨자의 코드는 끝 4자리만 보낸다. */
public record AdminDropResponse(
        Long id,
        String title,
        DropEventStatus status,
        DropPhase phase,
        Instant openAt,
        Instant closeAt,
        long registeredCodes,
        long assignedCodes,
        String prizeDescription,
        long entryAttempts,
        List<WinnerResponse> winners,
        Instant createdAt
) {
    public record WinnerResponse(String nickname, String codeHint, Instant assignedAt) {
    }

    public static AdminDropResponse from(AdminDropView view) {
        return new AdminDropResponse(
                view.event().getId(), view.event().getTitle(), view.event().getStatus(), view.phase(),
                view.event().getOpenAt(), view.event().getCloseAt(),
                view.registeredCodes(), view.assignedCodes(),
                view.event().getPrizeDescription(), view.entryAttempts(),
                view.winners().stream()
                        .map(w -> new WinnerResponse(w.nickname(), w.codeHint(), w.assignedAt()))
                        .toList(),
                view.event().getCreatedAt());
    }
}
