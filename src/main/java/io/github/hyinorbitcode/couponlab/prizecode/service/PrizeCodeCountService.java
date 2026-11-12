package io.github.hyinorbitcode.couponlab.prizecode.service;

import io.github.hyinorbitcode.couponlab.drop.PrizeCodeCounter;
import io.github.hyinorbitcode.couponlab.prizecode.repository.PrizeCodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PrizeCodeCountService implements PrizeCodeCounter {

    private final PrizeCodeRepository prizeCodeRepository;

    @Override
    @Transactional(readOnly = true)
    public long countByDropEventId(long dropEventId) {
        return prizeCodeRepository.countByDropEventId(dropEventId);
    }

}
