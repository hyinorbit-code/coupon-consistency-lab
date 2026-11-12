package io.github.hyinorbitcode.couponlab.prizecode.service;

import io.github.hyinorbitcode.couponlab.prizecode.AssignedPrize;
import io.github.hyinorbitcode.couponlab.prizecode.PrizeCodeAssigner;
import io.github.hyinorbitcode.couponlab.prizecode.entity.PrizeCode;
import io.github.hyinorbitcode.couponlab.prizecode.exception.SoldOutException;
import io.github.hyinorbitcode.couponlab.prizecode.repository.PrizeCodeRepository;
import io.github.hyinorbitcode.couponlab.prizecode.security.PrizeCodeCipher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PrizeCodeAssignService implements PrizeCodeAssigner {

    private final PrizeCodeRepository prizeCodeRepository;
    private final PrizeCodeCipher cipher;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public Optional<AssignedPrize> findAssigned(long dropEventId, long userId) {
        return prizeCodeRepository.findByDropEventIdAndAssignedUserId(dropEventId, userId)
                .map(this::toAssignedPrize);
    }

    @Override
    @Transactional
    public AssignedPrize assign(long dropEventId, long userId) {
        PrizeCode code = prizeCodeRepository
                .findFirstForUpdateByDropEventIdAndAssignedUserIdIsNullOrderByIdAsc(dropEventId)
                .orElseThrow(SoldOutException::new);
        code.assignTo(userId, clock.instant());
        return toAssignedPrize(code);
    }

    private AssignedPrize toAssignedPrize(PrizeCode code) {
        return new AssignedPrize(code.getId(), code.getDropEventId(),
                cipher.decrypt(code.getCodeCiphertext()), code.getAssignedAt());
    }
}