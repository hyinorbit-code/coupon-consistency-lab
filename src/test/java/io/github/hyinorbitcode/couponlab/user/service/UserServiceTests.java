package io.github.hyinorbitcode.couponlab.user.service;

import io.github.hyinorbitcode.couponlab.DatabaseCleaner;
import io.github.hyinorbitcode.couponlab.IntegrationTest;
import io.github.hyinorbitcode.couponlab.user.Role;
import io.github.hyinorbitcode.couponlab.user.entity.IdentityProvider;
import io.github.hyinorbitcode.couponlab.user.entity.User;
import io.github.hyinorbitcode.couponlab.user.entity.UserIdentity;
import io.github.hyinorbitcode.couponlab.user.entity.UserStatus;
import io.github.hyinorbitcode.couponlab.user.repository.UserIdentityRepository;
import io.github.hyinorbitcode.couponlab.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class UserServiceTests {

    private static final IdentityProvider PROVIDER = IdentityProvider.EXPERIMENT;

    @Autowired
    UserService userService;

    @Autowired
    UserRepository userRepository;

    @Autowired
    UserIdentityRepository userIdentityRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        DatabaseCleaner.truncateAll(jdbcTemplate);
    }

    @Test
    @DisplayName("처음 로그인하면 사용자와 연동 계정을 함께 만든다")
    void createsUserAndIdentityOnFirstLogin(){
        User user = userService.findOrCreate(PROVIDER, "u-1", "tester", Role.USER);

        assertThat(user.getId()).isNotNull();
        assertThat(user.getNickname()).isEqualTo("tester");
        assertThat(user.getRole()).isEqualTo(Role.USER);
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);

        UserIdentity identity = userIdentityRepository.findByProviderAndProviderUserId(PROVIDER, "u-1").orElseThrow();
        assertThat(identity.getUser().getId()).isEqualTo(user.getId());
        assertThat(userRepository.count()).isEqualTo(1);
        assertThat(userIdentityRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("이미 연동된 계정으로 다시 로그인하면 같은 사용자를 반환하고 새로 만들지 않는다")
    void returnsExistingUserOnSecondLogin() {
        User first = userService.findOrCreate(PROVIDER, "u-1", "tester", Role.USER);

        User second = userService.findOrCreate(PROVIDER, "u-1", "tester", Role.USER);

        assertThat(second.getId()).isEqualTo(first.getId());
        assertThat(userRepository.count()).isEqualTo(1);
        assertThat(userIdentityRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("다시 로그인할 때 다른 닉네임과 역할을 보내도 기존 값을 바꾸지 않는다")
    void keepsNicknameAndRoleOfExistingUser() {
        User first = userService.findOrCreate(PROVIDER, "u-1", "tester", Role.USER);

        User second = userService.findOrCreate(PROVIDER, "u-1", "changed", Role.ADMIN);

        assertThat(second.getNickname()).isEqualTo("tester");
        assertThat(second.getRole()).isEqualTo(Role.USER);
        User saved = userRepository.findById(first.getId()).orElseThrow();
        assertThat(saved.getNickname()).isEqualTo("tester");
        assertThat(saved.getRole()).isEqualTo(Role.USER);
    }

    @Test
    @DisplayName("제공자가 다르면 같은 식별자라도 다른 사용자다")
    void separatesUsersByProvider() {
        User experiment = userService.findOrCreate(IdentityProvider.EXPERIMENT, "u-1", "tester", Role.USER);

        User kakao = userService.findOrCreate(IdentityProvider.KAKAO, "u-1", "tester", Role.USER);

        assertThat(kakao.getId()).isNotEqualTo(experiment.getId());
        assertThat(userRepository.count()).isEqualTo(2);
    }
}
