package io.github.hyinorbitcode.couponlab.user.internal.application;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(long userId) {
        super("user not found: " + userId);
    }
}
