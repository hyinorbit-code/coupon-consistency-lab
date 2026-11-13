package io.github.hyinorbitcode.couponlab.participation.service;

import io.github.hyinorbitcode.couponlab.drop.DropEventApi;
import io.github.hyinorbitcode.couponlab.drop.DropMetrics;
import io.github.hyinorbitcode.couponlab.drop.PublishedDrop;
import io.github.hyinorbitcode.couponlab.prizecode.AssignedPrize;
import io.github.hyinorbitcode.couponlab.prizecode.PrizeCodeAssigner;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ParticipationService {

    private final DropEventApi dropEventApi;
    private final PrizeCodeAssigner prizeCodeAssigner;
    private final MeterRegistry meterRegistry;

    public EntryResult enter(long dropEventId, long userId) {
        PublishedDrop drop = dropEventApi.getPublishedDrop(dropEventId);
        meterRegistry.counter(DropMetrics.ENTRY_ATTEMPTS, DropMetrics.DROP_ID_TAG, String.valueOf(dropEventId)).increment();

        Optional<AssignedPrize> already = prizeCodeAssigner.findAssigned(dropEventId, userId);
        if (already.isPresent()) {
            return new EntryResult(drop, already.get(), false);
        }

        dropEventApi.checkEntryOpen(dropEventId);

        try {
            AssignedPrize prize = prizeCodeAssigner.assign(dropEventId, userId);
            return new EntryResult(drop, prize, true);
        } catch (DataIntegrityViolationException duplicate) {
            AssignedPrize existing = prizeCodeAssigner.findAssigned(dropEventId, userId)
                    .orElseThrow(() -> duplicate);
            return new EntryResult(drop, existing, false);
        }
    }
}