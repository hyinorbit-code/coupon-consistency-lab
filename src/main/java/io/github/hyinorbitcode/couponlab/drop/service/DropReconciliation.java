package io.github.hyinorbitcode.couponlab.drop.service;

import io.github.hyinorbitcode.couponlab.drop.CodeAssignmentStats;

/**
 * 이벤트 하나의 기본 대사 결과. 불변식이 지켜졌는지 DB 집계로 확인한다.
 *
 * @param assignedWithinRegistered 할당된 코드 수 ≤ 등록된 코드 수
 * @param oneWinPerUser            당첨자 수 = 할당된 코드 수 (한 사람이 두 번 당첨되지 않음)
 * @param assignmentsComplete      사용자와 할당 시각이 항상 함께 있음
 * @param assignedWithinPeriod     모든 할당이 [오픈, 종료) 안에서 일어남
 */
public record DropReconciliation(
        long dropEventId,
        CodeAssignmentStats stats,
        boolean assignedWithinRegistered,
        boolean oneWinPerUser,
        boolean assignmentsComplete,
        boolean assignedWithinPeriod
) {
    public static DropReconciliation of(long dropEventId, CodeAssignmentStats stats) {
        return new DropReconciliation(
                dropEventId,
                stats,
                stats.assignedCodes() <= stats.registeredCodes(),
                stats.distinctWinners() == stats.assignedCodes(),
                stats.incompleteAssignments() == 0,
                stats.assignedOutsidePeriod() == 0);
    }

    public boolean consistent() {
        return assignedWithinRegistered && oneWinPerUser && assignmentsComplete && assignedWithinPeriod;
    }
}
