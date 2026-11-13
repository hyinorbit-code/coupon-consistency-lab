package io.github.hyinorbitcode.couponlab.drop.dto.response;

import io.github.hyinorbitcode.couponlab.drop.service.DropReconciliation;

/** 기본 대사 결과. consistent가 false면 아래 항목 중 하나 이상이 어긋난 것이다. */
public record ReconciliationResponse(
        long dropId,
        boolean consistent,
        long registeredCodes,
        long assignedCodes,
        long distinctWinners,
        long incompleteAssignments,
        long assignedOutsidePeriod,
        boolean assignedWithinRegistered,
        boolean oneWinPerUser,
        boolean assignmentsComplete,
        boolean assignedWithinPeriod
) {
    public static ReconciliationResponse from(DropReconciliation r) {
        return new ReconciliationResponse(
                r.dropEventId(), r.consistent(),
                r.stats().registeredCodes(), r.stats().assignedCodes(), r.stats().distinctWinners(),
                r.stats().incompleteAssignments(), r.stats().assignedOutsidePeriod(),
                r.assignedWithinRegistered(), r.oneWinPerUser(), r.assignmentsComplete(), r.assignedWithinPeriod());
    }
}
