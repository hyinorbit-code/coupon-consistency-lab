package io.github.hyinorbitcode.couponlab.user.controller;

import io.github.hyinorbitcode.couponlab.user.CurrentUser;
import io.github.hyinorbitcode.couponlab.user.dto.response.MeResponse;
import io.github.hyinorbitcode.couponlab.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
class UserController {

    private final UserService userService;

    @GetMapping("/me")
    MeResponse me(CurrentUser currentUser) {
        return MeResponse.from(userService.get(currentUser.id()));
    }
}
