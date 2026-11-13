package io.github.hyinorbitcode.couponlab.drop.controller;

import io.github.hyinorbitcode.couponlab.drop.dto.request.CreateDropRequest;
import io.github.hyinorbitcode.couponlab.drop.dto.response.AdminDropResponse;
import io.github.hyinorbitcode.couponlab.drop.dto.response.AdminDropSummaryResponse;
import io.github.hyinorbitcode.couponlab.drop.dto.response.ReconciliationResponse;
import io.github.hyinorbitcode.couponlab.drop.entity.DropEvent;
import io.github.hyinorbitcode.couponlab.drop.service.DropService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/drops")
@RequiredArgsConstructor
class AdminDropController {

    private final DropService dropService;

    @GetMapping
    List<AdminDropSummaryResponse> list() {
        return dropService.listForAdmin().stream().map(AdminDropSummaryResponse::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    AdminDropResponse create(@Valid @RequestBody CreateDropRequest request) {
        DropEvent event = dropService.create(
                request.title(), request.prizeDescription(), request.openAt(), request.closeAt());
        return AdminDropResponse.from(dropService.getAdminView(event.getId()));
    }

    @GetMapping("/{id}")
    AdminDropResponse get(@PathVariable long id) {
        return AdminDropResponse.from(dropService.getAdminView(id));
    }

    @PostMapping("/{id}/publish")
    AdminDropResponse publish(@PathVariable long id) {
        dropService.publish(id);
        return AdminDropResponse.from(dropService.getAdminView(id));
    }

    @PostMapping("/{id}/cancel")
    AdminDropResponse cancel(@PathVariable long id) {
        dropService.cancel(id);
        return AdminDropResponse.from(dropService.getAdminView(id));
    }

    @GetMapping("/{id}/reconciliation")
    ReconciliationResponse reconcile(@PathVariable long id) {
        return ReconciliationResponse.from(dropService.reconcile(id));
    }
}
