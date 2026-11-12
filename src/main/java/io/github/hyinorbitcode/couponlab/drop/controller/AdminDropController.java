package io.github.hyinorbitcode.couponlab.drop.controller;

import io.github.hyinorbitcode.couponlab.drop.dto.request.CreateDropRequest;
import io.github.hyinorbitcode.couponlab.drop.dto.response.AdminDropResponse;
import io.github.hyinorbitcode.couponlab.drop.entity.DropEvent;
import io.github.hyinorbitcode.couponlab.drop.service.DropService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.Clock;

@RestController
@RequestMapping("/admin/drops")
@RequiredArgsConstructor
class AdminDropController {

    private final DropService dropService;
    private final Clock clock;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    AdminDropResponse create(@Valid @RequestBody CreateDropRequest request) {
        DropEvent event = dropService.create(
                request.title(), request.prizeDescription(), request.openAt(), request.closeAt());
        return AdminDropResponse.from(event, clock.instant());
    }

    @GetMapping("/{id}")
    AdminDropResponse get(@PathVariable long id) {
        return AdminDropResponse.from(dropService.getForAdmin(id), clock.instant());
    }

    @PostMapping("/{id}/publish")
    AdminDropResponse publish(@PathVariable long id) {
        return AdminDropResponse.from(dropService.publish(id), clock.instant());
    }

    @PostMapping("/{id}/cancel")
    AdminDropResponse cancel(@PathVariable long id) {
        return AdminDropResponse.from(dropService.cancel(id), clock.instant());
    }
}
