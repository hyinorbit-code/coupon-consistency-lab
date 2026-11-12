package io.github.hyinorbitcode.couponlab.prizecode.repository;

import io.github.hyinorbitcode.couponlab.prizecode.entity.PrizeCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PrizeCodeRepository extends JpaRepository<PrizeCode, Long> {

    boolean existsByCodeHash(byte[] codeHash);

    long countByDropEventId(long dropEventId);

    Optional<PrizeCode> findByDropEventIdAndAssignedUserId(long dropEventId, long assignedUserId);

    Optional<PrizeCode> findFirstByDropEventIdAndAssignedUserIdIsNullOrderByIdAsc(long dropEventId);
}