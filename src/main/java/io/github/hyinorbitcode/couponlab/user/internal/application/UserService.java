package io.github.hyinorbitcode.couponlab.user.internal.application;

import io.github.hyinorbitcode.couponlab.user.Role;
import io.github.hyinorbitcode.couponlab.user.internal.domain.IdentityProvider;
import io.github.hyinorbitcode.couponlab.user.internal.domain.User;
import io.github.hyinorbitcode.couponlab.user.internal.domain.UserIdentity;
import io.github.hyinorbitcode.couponlab.user.internal.domain.UserIdentityRepository;
import io.github.hyinorbitcode.couponlab.user.internal.domain.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserIdentityRepository userIdentityRepository;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    /**
     * 외부 계정으로 연동된 사용자가 있으면 그대로 반환하고, 없으면 사용자와 연동 계정을 함께 만든다.
     * 기존 사용자의 닉네임·역할은 바꾸지 않는다.
     *
     * <p>같은 외부 계정의 첫 로그인이 동시에 들어오면 둘 다 "없음"을 보고 생성할 수 있다.
     * {@code UNIQUE(provider, provider_user_id)}가 한쪽을 막고, 막힌 쪽의 생성 트랜잭션(사용자 + 연동 계정)은
     * 통째로 롤백된다. 막힌 쪽은 새 트랜잭션에서 먼저 커밋된 사용자를 다시 조회해 반환한다.
     */
    public User findOrCreate(IdentityProvider provider, String providerUserId, String nickname, Role role) {
        return findByIdentity(provider, providerUserId)
                .orElseGet(() -> create(provider, providerUserId, nickname, role));
    }

    public User get(long userId) {
        return userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
    }

    private User create(IdentityProvider provider, String providerUserId, String nickname, Role role) {
        try {
            return transactionTemplate.execute(status -> {
                Instant now = clock.instant();
                User user = userRepository.save(User.createWithoutCredentials(nickname, role, now));
                userIdentityRepository.saveAndFlush(UserIdentity.link(user.getId(), provider, providerUserId, now));
                return user;
            });
        } catch (DataIntegrityViolationException duplicate) {
            return findByIdentity(provider, providerUserId).orElseThrow(() -> duplicate);
        }
    }

    private Optional<User> findByIdentity(IdentityProvider provider, String providerUserId) {
        return userIdentityRepository.findByProviderAndProviderUserId(provider, providerUserId)
                .flatMap(identity -> userRepository.findById(identity.getUserId()));
    }
}
