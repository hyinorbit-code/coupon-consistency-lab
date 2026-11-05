package io.github.hyinorbitcode.couponlab.user.service;

import io.github.hyinorbitcode.couponlab.user.Role;
import io.github.hyinorbitcode.couponlab.user.entity.IdentityProvider;
import io.github.hyinorbitcode.couponlab.user.entity.User;
import io.github.hyinorbitcode.couponlab.user.entity.UserIdentity;
import io.github.hyinorbitcode.couponlab.user.repository.UserIdentityRepository;
import io.github.hyinorbitcode.couponlab.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserIdentityRepository userIdentityRepository;
    private final Clock clock;
    private final TransactionTemplate transactionTemplate;

    public User findOrCreate(IdentityProvider provider, String providerUserId, String nickname, Role role) {
        return findByIdentity(provider, providerUserId)
                .orElseGet(() -> create(provider, providerUserId, nickname, role));
    }

    private User create(IdentityProvider provider, String providerUserId, String nickname, Role role) {

        try{
            return transactionTemplate.execute(status -> {
                Instant now = clock.instant();
                User user = userRepository.save(User.createWithoutCredentials(nickname, role, now));
                userIdentityRepository.saveAndFlush(UserIdentity.link(user, provider, providerUserId, now));
                return user;
            });
        }catch (DataIntegrityViolationException duplicate){
            return findByIdentity(provider, providerUserId).orElseThrow(() -> duplicate);
        }
    }

    private Optional<User> findByIdentity(IdentityProvider provider, String providerUserId) {
        return userIdentityRepository.findByProviderAndProviderUserId(provider, providerUserId)
                .map(UserIdentity::getUser);
    }
}
