package io.github.hyinorbitcode.couponlab.user.dto.response;

import io.github.hyinorbitcode.couponlab.user.Role;

public record UserResponse(long id, String nickname, Role role) {
}
