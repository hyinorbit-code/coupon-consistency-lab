package io.github.hyinorbitcode.couponlab.drop;

public interface PrizeCodeCounter {
    long countByDropEventId(long dropEventId);
}