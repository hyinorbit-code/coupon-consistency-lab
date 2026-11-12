package io.github.hyinorbitcode.couponlab.prizecode;

import java.util.Optional;

public interface PrizeCodeAssigner {
    Optional<AssignedPrize> findAssigned(long dropEventId, long userId);
    AssignedPrize assign(long dropEventId, long userId);
}
