package io.github.hyinorbitcode.couponlab.drop.dto.request;

import io.github.hyinorbitcode.couponlab.drop.entity.DropEvent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record CreateDropRequest(
        @NotBlank @Size(max = DropEvent.MAX_TITLE_LENGTH) String title,
        @NotBlank @Size(max = DropEvent.MAX_PRIZE_DESCRIPTION_LENGTH) String prizeDescription,
        @NotNull Instant openAt,
        @NotNull Instant closeAt
) {
}