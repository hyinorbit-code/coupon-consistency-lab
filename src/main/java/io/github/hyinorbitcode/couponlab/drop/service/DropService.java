package io.github.hyinorbitcode.couponlab.drop.service;

import io.github.hyinorbitcode.couponlab.drop.DropEventApi;
import io.github.hyinorbitcode.couponlab.drop.PrizeCodeCounter;
import io.github.hyinorbitcode.couponlab.drop.entity.DropEvent;
import io.github.hyinorbitcode.couponlab.drop.exception.CodesLockedException;
import io.github.hyinorbitcode.couponlab.drop.exception.DropNotFoundException;
import io.github.hyinorbitcode.couponlab.drop.exception.NoCodesException;
import io.github.hyinorbitcode.couponlab.drop.repository.DropEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class DropService implements DropEventApi {

    private final DropEventRepository dropEventRepository;
    private final PrizeCodeCounter prizeCodeCounter;
    private final Clock clock;

    @Transactional
    public DropEvent create(String title, String prizeDescription, Instant openAt, Instant closeAt) {
        DropEvent event = DropEvent.create(title, prizeDescription, openAt, closeAt, clock.instant());
        return dropEventRepository.save(event);
    }

    @Transactional(readOnly = true)
    public DropEvent getForAdmin(long id) {
        return dropEventRepository.findById(id)
                .orElseThrow(DropNotFoundException::new);
    }

    @Override
    @Transactional(readOnly = true)
    public void checkCodesRegistrable(long dropEventId) {
        DropEvent event = getForAdmin(dropEventId);
        if (!event.canRegisterCodes(clock.instant())) {
            throw new CodesLockedException();
        }
    }

    @Transactional
    public DropEvent publish(long id) {
        DropEvent event = getForAdmin(id);
        if (prizeCodeCounter.countByDropEventId(id) == 0) {
            throw new NoCodesException();
        }
        event.publish(clock.instant());
        return event;
    }

    @Transactional
    public DropEvent cancel(long id) {
        DropEvent event = getForAdmin(id);
        event.cancel(clock.instant());
        return event;
    }

}
