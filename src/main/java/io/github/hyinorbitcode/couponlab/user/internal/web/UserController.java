package io.github.hyinorbitcode.couponlab.user.internal.web;

import io.github.hyinorbitcode.couponlab.user.CurrentUser;
import io.github.hyinorbitcode.couponlab.user.internal.application.UserService;
import io.github.hyinorbitcode.couponlab.user.internal.domain.User;
import io.github.hyinorbitcode.couponlab.user.internal.web.dto.UserResponse;
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
    UserResponse me(CurrentUser currentUser) {
        User user = userService.get(currentUser.id());
        return new UserResponse(user.getId(), user.getNickname(), user.getRole());
    }
}
