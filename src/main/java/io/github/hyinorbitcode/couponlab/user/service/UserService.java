package io.github.hyinorbitcode.couponlab.user.service;

import io.github.hyinorbitcode.couponlab.user.Role;
import io.github.hyinorbitcode.couponlab.user.entity.IdentityProvider;
import io.github.hyinorbitcode.couponlab.user.entity.User;
import io.github.hyinorbitcode.couponlab.user.entity.UserIdentity;
import io.github.hyinorbitcode.couponlab.user.repository.UserIdentityRepository;
import io.github.hyinorbitcode.couponlab.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserIdentityRepository userIdentityRepository;
    private final Clock clock;

    @Transactional
    public User findOrCreate(IdentityProvider provider, String providerUserId, String nickname, Role role) {
        return userIdentityRepository.findByProviderAndProviderUserId(provider, providerUserId)
                .map(UserIdentity::getUser)
                .orElseGet(() -> create(provider, providerUserId, nickname, role));
    }

    private User create(IdentityProvider provider, String providerUserId, String nickname, Role role) {
        Instant now = clock.instant();
        User user = userRepository.save(User.createWithoutCredentials(nickname, role, now));
        userIdentityRepository.save(UserIdentity.link(user, provider, providerUserId, now));
        return user;
    }
}
