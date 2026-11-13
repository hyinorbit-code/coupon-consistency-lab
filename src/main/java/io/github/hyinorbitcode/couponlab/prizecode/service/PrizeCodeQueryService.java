package io.github.hyinorbitcode.couponlab.prizecode.service;

import io.github.hyinorbitcode.couponlab.drop.CodeAssignmentStats;
import io.github.hyinorbitcode.couponlab.drop.DropWinner;
import io.github.hyinorbitcode.couponlab.drop.PrizeCodeQuery;
import io.github.hyinorbitcode.couponlab.prizecode.entity.PrizeCode;
import io.github.hyinorbitcode.couponlab.prizecode.repository.PrizeCodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * drop 모듈의 {@link PrizeCodeQuery} 구현. 리포지토리만 쓴다.
 * {@link PrizeCodeService}는 drop 서비스를 주입받으므로 여기서 구현하면 빈 순환이 생긴다.
 */
@Service
@RequiredArgsConstructor
public class PrizeCodeQueryService implements PrizeCodeQuery {

    private final PrizeCodeRepository prizeCodeRepository;

    @Override
    @Transactional(readOnly = true)
    public long countByDropEventId(long dropEventId) {
        return prizeCodeRepository.countByDropEventId(dropEventId);
    }

    @Override
    @Transactional(readOnly = true)
    public long countRemainingByDropEventId(long dropEventId) {
        return prizeCodeRepository.countByDropEventIdAndAssignedUserIdIsNull(dropEventId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Long> findPrizeIdOf(long dropEventId, long userId) {
        return prizeCodeRepository.findByDropEventIdAndAssignedUserId(dropEventId, userId)
                .map(PrizeCode::getId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DropWinner> findWinners(long dropEventId) {
        return prizeCodeRepository.findByDropEventIdAndAssignedUserIdIsNotNullOrderByAssignedAtAscIdAsc(dropEventId)
                .stream()
                .map(code -> new DropWinner(code.getAssignedUserId(), code.getCodeHint(), code.getAssignedAt()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CodeAssignmentStats statsOf(long dropEventId, Instant openAt, Instant closeAt) {
        return new CodeAssignmentStats(
                prizeCodeRepository.countByDropEventId(dropEventId),
                prizeCodeRepository.countByDropEventIdAndAssignedUserIdIsNotNull(dropEventId),
                prizeCodeRepository.countDistinctWinners(dropEventId),
                prizeCodeRepository.countIncompleteAssignments(dropEventId),
                prizeCodeRepository.countAssignedOutside(dropEventId, openAt, closeAt));
    }
}
