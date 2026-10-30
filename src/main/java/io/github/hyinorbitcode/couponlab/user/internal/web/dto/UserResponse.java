package io.github.hyinorbitcode.couponlab.user.internal.web.dto;

import io.github.hyinorbitcode.couponlab.user.Role;

public record UserResponse(long id, String nickname, Role role) {
}
