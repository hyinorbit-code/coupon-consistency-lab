package io.github.hyinorbitcode.couponlab.user.dto.response;

import io.github.hyinorbitcode.couponlab.user.Role;
import io.github.hyinorbitcode.couponlab.user.entity.User;

public record MeResponse(Long id, String nickname, Role role) {

    public static MeResponse from(User user) {
        return new MeResponse(user.getId(), user.getNickname(), user.getRole());
    }
}
