package io.github.hyinorbitcode.couponlab.drop.service;

import io.github.hyinorbitcode.couponlab.drop.entity.DropEvent;
import io.github.hyinorbitcode.couponlab.drop.exception.DropNotFoundException;
import io.github.hyinorbitcode.couponlab.drop.repository.DropEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class DropService {

    private final DropEventRepository dropEventRepository;
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

}
