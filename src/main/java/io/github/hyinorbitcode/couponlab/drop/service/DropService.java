package io.github.hyinorbitcode.couponlab.drop.service;

import io.github.hyinorbitcode.couponlab.drop.CodeAssignmentStats;
import io.github.hyinorbitcode.couponlab.drop.DropEventApi;
import io.github.hyinorbitcode.couponlab.drop.DropMetrics;
import io.github.hyinorbitcode.couponlab.drop.DropWinner;
import io.github.hyinorbitcode.couponlab.drop.PrizeCodeQuery;
import io.github.hyinorbitcode.couponlab.drop.PublishedDrop;
import io.github.hyinorbitcode.couponlab.drop.entity.DropEvent;
import io.github.hyinorbitcode.couponlab.drop.entity.DropEventStatus;
import io.github.hyinorbitcode.couponlab.drop.exception.*;
import io.github.hyinorbitcode.couponlab.drop.repository.DropEventRepository;
import io.github.hyinorbitcode.couponlab.user.UserNicknames;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.data.domain.Sort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DropService implements DropEventApi {

    private final DropEventRepository dropEventRepository;
    private final PrizeCodeQuery prizeCodeQuery;
    private final UserNicknames userNicknames;
    private final MeterRegistry meterRegistry;
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
        if (prizeCodeQuery.countByDropEventId(id) == 0) {
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

    @Override
    @Transactional(readOnly = true)
    public PublishedDrop getPublishedDrop(long dropEventId) {
        DropEvent event = findPublished(dropEventId);
        return new PublishedDrop(event.getId(), event.getTitle(), event.getPrizeDescription());
    }

    @Override
    @Transactional(readOnly = true)
    public void checkEntryOpen(long dropEventId) {
        DropEvent event = getForAdmin(dropEventId);
        switch (event.phase(clock.instant())) {
            case UPCOMING -> throw new DropNotOpenException();
            case CLOSED -> throw new DropClosedException();
            default -> { }
        }
    }

    @Transactional(readOnly = true)
    public List<DropView> listPublished() {
        Instant now = clock.instant();
        return dropEventRepository.findByStatusOrderByOpenAtAscIdAsc(DropEventStatus.PUBLISHED).stream()
                .map(event -> toView(event, now, null))
                .toList();
    }

    @Transactional(readOnly = true)
    public DropView getPublished(long id, Optional<Long> userId) {
        DropEvent event = findPublished(id);
        Long myPrizeId = userId.flatMap(uid -> prizeCodeQuery.findPrizeIdOf(id, uid)).orElse(null);
        return toView(event, clock.instant(), myPrizeId);
    }

    @Transactional(readOnly = true)
    public DropView getStock(long id) {
        return toView(findPublished(id), clock.instant(), null);
    }

    private DropEvent findPublished(long id) {
        DropEvent event = getForAdmin(id);
        if (event.getStatus() != DropEventStatus.PUBLISHED) {
            throw new DropNotFoundException();
        }
        return event;
    }

    private DropView toView(DropEvent event, Instant now, Long myPrizeId) {
        long total = prizeCodeQuery.countByDropEventId(event.getId());
        long remaining = prizeCodeQuery.countRemainingByDropEventId(event.getId());
        return new DropView(event, total, remaining, event.phase(now, remaining), myPrizeId);
    }

    @Transactional(readOnly = true)
    public List<AdminDropView> listForAdmin() {
        Instant now = clock.instant();
        return dropEventRepository.findAll(Sort.by(Sort.Order.desc("openAt"), Sort.Order.desc("id"))).stream()
                .map(event -> toAdminView(event, now, List.of()))
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminDropView getAdminView(long id) {
        DropEvent event = getForAdmin(id);
        List<DropWinner> winners = prizeCodeQuery.findWinners(id);
        Map<Long, String> nicknames = userNicknames.findNicknames(
                winners.stream().map(DropWinner::userId).distinct().toList());
        List<AdminDropView.Winner> winnerViews = winners.stream()
                .map(w -> new AdminDropView.Winner(nicknames.getOrDefault(w.userId(), "(알 수 없음)"), w.codeHint(), w.assignedAt()))
                .toList();
        return toAdminView(event, clock.instant(), winnerViews);
    }

    @Transactional(readOnly = true)
    public DropReconciliation reconcile(long id) {
        DropEvent event = getForAdmin(id);
        CodeAssignmentStats stats = prizeCodeQuery.statsOf(id, event.getOpenAt(), event.getCloseAt());
        return DropReconciliation.of(id, stats);
    }

    private AdminDropView toAdminView(DropEvent event, Instant now, List<AdminDropView.Winner> winners) {
        long registered = prizeCodeQuery.countByDropEventId(event.getId());
        long remaining = prizeCodeQuery.countRemainingByDropEventId(event.getId());
        return new AdminDropView(event, event.phase(now, remaining), registered, registered - remaining,
                entryAttempts(event.getId()), winners);
    }

    private long entryAttempts(long dropEventId) {
        Counter counter = meterRegistry.find(DropMetrics.ENTRY_ATTEMPTS)
                .tag(DropMetrics.DROP_ID_TAG, String.valueOf(dropEventId))
                .counter();
        return counter == null ? 0 : (long) counter.count();
    }

}
