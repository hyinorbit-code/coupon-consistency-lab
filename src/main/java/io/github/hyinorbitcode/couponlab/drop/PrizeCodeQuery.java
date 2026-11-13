package io.github.hyinorbitcode.couponlab.drop;

import java.util.Optional;

/**
 * drop 모듈이 경품 코드에 대해 묻는 질문. 구현은 prizecode 모듈이 한다.
 * drop이 prizecode를 직접 참조하지 않게 해서 모듈 간 순환 의존을 막는다.
 */
public interface PrizeCodeQuery {

    /** 이벤트에 등록된 코드 수. */
    long countByDropEventId(long dropEventId);

    /** 아직 할당되지 않은 코드 수. */
    long countRemainingByDropEventId(long dropEventId);

    /** 사용자가 이 이벤트에서 받은 경품 코드 ID. 당첨되지 않았으면 비어 있다. */
    Optional<Long> findPrizeIdOf(long dropEventId, long userId);
}
