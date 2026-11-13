package io.github.hyinorbitcode.couponlab.drop.controller;

import io.github.hyinorbitcode.couponlab.drop.dto.response.DropDetailResponse;
import io.github.hyinorbitcode.couponlab.drop.dto.response.DropStockResponse;
import io.github.hyinorbitcode.couponlab.drop.dto.response.DropSummaryResponse;
import io.github.hyinorbitcode.couponlab.drop.service.DropService;
import io.github.hyinorbitcode.couponlab.user.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

/** 사용자용 이벤트 조회. 로그인하지 않아도 볼 수 있다. */
@RestController
@RequestMapping("/drops")
@RequiredArgsConstructor
class DropController {

    private final DropService dropService;

    @GetMapping
    List<DropSummaryResponse> list() {
        return dropService.listPublished().stream().map(DropSummaryResponse::from).toList();
    }

    @GetMapping("/{id}")
    DropDetailResponse get(@PathVariable long id, Optional<CurrentUser> currentUser) {
        return DropDetailResponse.from(dropService.getPublished(id, currentUser.map(CurrentUser::id)));
    }

    @GetMapping("/{id}/stock")
    DropStockResponse stock(@PathVariable long id) {
        return DropStockResponse.from(dropService.getStock(id));
    }
}
