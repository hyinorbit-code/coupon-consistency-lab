package io.github.hyinorbitcode.couponlab.user.entity;

import io.github.hyinorbitcode.couponlab.user.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserIdentityTests {

    private static final Instant NOW = Instant.parse("2026-11-03T00:00:00Z");
    private static final IdentityProvider PROVIDER = IdentityProvider.KAKAO;

    private static User newUser() {
        return User.createWithoutCredentials("tester", Role.USER, NOW);
    }

    @Test
    @DisplayName("외부 계정을 연동하면 전달한 사용자, 제공자, 식별자, 시각을 가진다")
    void linkIdentity(){

        User user = newUser();
        String providerUserId = "12345";

        UserIdentity identity = UserIdentity.link(user,PROVIDER,providerUserId,NOW);

        assertThat(identity.getId()).isNull();
        assertThat(identity.getUser()).isSameAs(user);
        assertThat(identity.getProvider()).isEqualTo(PROVIDER);
        assertThat(identity.getProviderUserId()).isEqualTo(providerUserId);
        assertThat(identity.getLinkedAt()).isEqualTo(NOW);

    }

    @Test
    @DisplayName("제공자 식별자는 최대 길이까지 허용한다")
    void acceptsProviderUserIdAtMaxLength() {
        User user = newUser();
        String providerUserId = "1".repeat(UserIdentity.MAX_PROVIDER_USER_ID_LENGTH);

        UserIdentity identity = UserIdentity.link(user, PROVIDER, providerUserId, NOW);

        assertThat(identity.getProviderUserId()).isEqualTo(providerUserId);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   "})
    @DisplayName("제공자 식별자가 비어 있으면 연동할 수 없다")
    void rejectsBlankProviderUserId(String providerUserId) {
        User user = newUser();

        assertThatThrownBy(() -> UserIdentity.link(user, PROVIDER, providerUserId, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("제공자 식별자가 최대 길이를 넘으면 연동할 수 없다")
    void rejectsTooLongProviderUserId() {
        User user = newUser();

        String providerUserId = "1".repeat(UserIdentity.MAX_PROVIDER_USER_ID_LENGTH + 1);

        assertThatThrownBy(() -> UserIdentity.link(user, PROVIDER, providerUserId, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("사용자, 제공자, 시각이 없으면 연동할 수 없다")
    void rejectsNullArguments() {
        User user = newUser();

        String providerUserId = "12345";

        assertThatThrownBy(() -> UserIdentity.link(null, PROVIDER, providerUserId, NOW))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> UserIdentity.link(user, null, providerUserId, NOW))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> UserIdentity.link(user, PROVIDER, providerUserId, null))
                .isInstanceOf(NullPointerException.class);
    }

}
