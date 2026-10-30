package io.github.hyinorbitcode.couponlab.user.internal.application;

import io.github.hyinorbitcode.couponlab.user.Role;
import io.github.hyinorbitcode.couponlab.user.internal.domain.AuthProvider;
import io.github.hyinorbitcode.couponlab.user.internal.domain.User;
import io.github.hyinorbitcode.couponlab.user.internal.domain.UserRepository;
import java.time.Clock;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final Clock clock;

    UserService(UserRepository userRepository, Clock clock) {
        this.userRepository = userRepository;
        this.clock = clock;
    }

    /**
     * 같은 인증 제공자·식별자의 사용자가 있으면 그대로 반환하고, 없으면 만든다.
     * 기존 사용자의 닉네임·역할은 바꾸지 않는다.
     *
     * <p>첫 로그인이 동시에 들어오면 둘 다 "없음"을 보고 INSERT할 수 있다. 유니크 제약이 한쪽을 막고,
     * 막힌 쪽은 먼저 커밋된 사용자를 다시 조회해 반환한다. 실패한 INSERT의 트랜잭션은 이미 롤백되므로
     * 재조회는 새 트랜잭션에서 실행되도록 이 메서드 전체를 트랜잭션으로 묶지 않는다.
     */
    public User findOrCreate(AuthProvider authProvider, String providerUserId, String nickname, Role role) {
        return userRepository.findByAuthProviderAndProviderUserId(authProvider, providerUserId)
                .orElseGet(() -> create(authProvider, providerUserId, nickname, role));
    }

    public User get(long userId) {
        return userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
    }

    private User create(AuthProvider authProvider, String providerUserId, String nickname, Role role) {
        User user = User.create(authProvider, providerUserId, nickname, role, clock.instant());
        try {
            return userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException duplicate) {
            return userRepository.findByAuthProviderAndProviderUserId(authProvider, providerUserId)
                    .orElseThrow(() -> duplicate);
        }
    }
}
