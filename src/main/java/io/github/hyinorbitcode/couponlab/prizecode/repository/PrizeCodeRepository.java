package io.github.hyinorbitcode.couponlab.prizecode.repository;

import io.github.hyinorbitcode.couponlab.prizecode.entity.PrizeCode;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface PrizeCodeRepository extends JpaRepository<PrizeCode, Long> {

    boolean existsByCodeHash(byte[] codeHash);

    long countByDropEventId(long dropEventId);

    Optional<PrizeCode> findByDropEventIdAndAssignedUserId(long dropEventId, long assignedUserId);

    Optional<PrizeCode> findFirstByDropEventIdAndAssignedUserIdIsNullOrderByIdAsc(long dropEventId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PrizeCode> findFirstForUpdateByDropEventIdAndAssignedUserIdIsNullOrderByIdAsc(long dropEventId);
}