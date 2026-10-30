package io.github.hyinorbitcode.couponlab.user.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserIdentityTests {

    private static final Instant NOW = Instant.parse("2026-11-01T00:00:00Z");

    @Test
    @DisplayName("제공자 회원번호는 비어 있을 수 없다")
    void rejectsBlankProviderUserId() {
        assertThatThrownBy(() -> UserIdentity.link(1L, IdentityProvider.KAKAO, " ", NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("제공자 회원번호는 64자를 넘을 수 없다")
    void rejectsTooLongProviderUserId() {
        String providerUserId = "1".repeat(UserIdentity.MAX_PROVIDER_USER_ID_LENGTH + 1);

        assertThatThrownBy(() -> UserIdentity.link(1L, IdentityProvider.KAKAO, providerUserId, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("사용자에 외부 계정을 연동한다")
    void linksIdentity() {
        UserIdentity identity = UserIdentity.link(1L, IdentityProvider.KAKAO, "123456789", NOW);

        assertThat(identity.getUserId()).isEqualTo(1L);
        assertThat(identity.getProvider()).isEqualTo(IdentityProvider.KAKAO);
        assertThat(identity.getLinkedAt()).isEqualTo(NOW);
    }
}
