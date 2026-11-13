package io.github.hyinorbitcode.couponlab.drop;

/**
 * 이벤트 하나의 코드 할당 집계. 기본 대사에 쓴다.
 *
 * @param registeredCodes       등록된 코드 수
 * @param assignedCodes         할당된 코드 수
 * @param distinctWinners       코드를 받은 서로 다른 사용자 수
 * @param incompleteAssignments 사용자와 할당 시각 중 하나만 채워진 행 수
 * @param assignedOutsidePeriod 이벤트 기간 [오픈, 종료) 밖에서 할당된 코드 수
 */
public record CodeAssignmentStats(
        long registeredCodes,
        long assignedCodes,
        long distinctWinners,
        long incompleteAssignments,
        long assignedOutsidePeriod
) {
}
