package io.github.hyinorbitcode.couponlab.drop;

import java.time.Instant;

/** 이벤트 당첨자 한 명. 코드 원문 대신 끝 4자리만 담는다. */
public record DropWinner(long userId, String codeHint, Instant assignedAt) {
}
