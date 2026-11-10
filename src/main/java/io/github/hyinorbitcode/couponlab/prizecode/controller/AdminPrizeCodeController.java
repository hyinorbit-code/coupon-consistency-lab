package io.github.hyinorbitcode.couponlab.prizecode.controller;

import io.github.hyinorbitcode.couponlab.prizecode.dto.request.RegisterCodesRequest;
import io.github.hyinorbitcode.couponlab.prizecode.dto.response.RegisterCodesResponse;
import io.github.hyinorbitcode.couponlab.prizecode.service.PrizeCodeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/drops/{dropId}/codes")
@RequiredArgsConstructor
class AdminPrizeCodeController {

    private final PrizeCodeService prizeCodeService;

    @PostMapping
    RegisterCodesResponse register(@PathVariable long dropId,
                                   @Valid @RequestBody RegisterCodesRequest request) {
        return RegisterCodesResponse.from(prizeCodeService.register(dropId, request.codes()));
    }
}
