package io.github.hyinorbitcode.couponlab.prizecode.dto.request;

import io.github.hyinorbitcode.couponlab.prizecode.entity.PrizeCode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record RegisterCodesRequest(
        @NotEmpty
        @Size(max = MAX_CODES_PER_REQUEST)
        List<@NotBlank @Size(max = PrizeCode.MAX_CODE_LENGTH) String> codes
) {
    public static final int MAX_CODES_PER_REQUEST = 1000;
}