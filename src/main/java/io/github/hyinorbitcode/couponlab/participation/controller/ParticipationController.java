package io.github.hyinorbitcode.couponlab.participation.controller;

import io.github.hyinorbitcode.couponlab.participation.dto.response.EntryResponse;
import io.github.hyinorbitcode.couponlab.participation.service.EntryResult;
import io.github.hyinorbitcode.couponlab.participation.service.ParticipationService;
import io.github.hyinorbitcode.couponlab.user.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/drops/{dropId}/entries")
@RequiredArgsConstructor
class ParticipationController {

    private final ParticipationService participationService;

    @PostMapping
    ResponseEntity<EntryResponse> enter(@PathVariable long dropId, CurrentUser currentUser) {
        EntryResult result = participationService.enter(dropId, currentUser.id());
        HttpStatus status = result.newlyWon() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(EntryResponse.won(result));
    }
}
