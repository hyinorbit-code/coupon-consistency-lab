package io.github.hyinorbitcode.couponlab.prizecode.repository;

import io.github.hyinorbitcode.couponlab.prizecode.entity.PrizeCode;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PrizeCodeRepository extends JpaRepository<PrizeCode, Long> {

    boolean existsByCodeHash(byte[] codeHash);

    long countByDropEventId(long dropEventId);

    long countByDropEventIdAndAssignedUserIdIsNull(long dropEventId);

    Optional<PrizeCode> findByDropEventIdAndAssignedUserId(long dropEventId, long assignedUserId);

    Optional<PrizeCode> findFirstByDropEventIdAndAssignedUserIdIsNullOrderByIdAsc(long dropEventId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PrizeCode> findFirstForUpdateByDropEventIdAndAssignedUserIdIsNullOrderByIdAsc(long dropEventId);

    @Query(value = """
            select * from prize_codes
            where drop_event_id = :dropEventId
              and assigned_user_id is null
            order by id
            limit 1
            for update skip locked
            """, nativeQuery = true)
    Optional<PrizeCode> findFirstAvailableForUpdateSkipLocked(@Param("dropEventId") long dropEventId);
}