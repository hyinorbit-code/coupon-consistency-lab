package io.github.hyinorbitcode.couponlab.user.internal;

import io.github.hyinorbitcode.couponlab.user.CurrentUser;
import io.github.hyinorbitcode.couponlab.user.Role;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/users")
class UserController {

    private final UserRepository userRepository;

    UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/me")
    UserResponse me(CurrentUser currentUser) {
        User user = userRepository.findById(currentUser.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "user not found"));
        return new UserResponse(user.getId(), user.getNickname(), user.getRole());
    }

    record UserResponse(long id, String nickname, Role role) {
    }
}
