package io.github.hyinorbitcode.couponlab.user.internal.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사용자에 연동된 외부 계정. (제공자, 제공자 회원번호)는 사용자 한 명에게만 연동되고, 사용자당 제공자는 하나다.
 */
@Entity
@Table(name = "user_identities")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserIdentity {

    public static final int MAX_PROVIDER_USER_ID_LENGTH = 64;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    @Enumerated(EnumType.STRING)
    private IdentityProvider provider;

    private String providerUserId;

    private Instant linkedAt;

    private UserIdentity(Long userId, IdentityProvider provider, String providerUserId, Instant linkedAt) {
        this.userId = userId;
        this.provider = provider;
        this.providerUserId = providerUserId;
        this.linkedAt = linkedAt;
    }

    public static UserIdentity link(Long userId, IdentityProvider provider, String providerUserId, Instant now) {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(provider, "provider");
        Objects.requireNonNull(now, "now");
        if (providerUserId == null || providerUserId.isBlank()) {
            throw new IllegalArgumentException("providerUserId must not be blank");
        }
        if (providerUserId.length() > MAX_PROVIDER_USER_ID_LENGTH) {
            throw new IllegalArgumentException(
                    "providerUserId must be at most " + MAX_PROVIDER_USER_ID_LENGTH + " characters");
        }
        return new UserIdentity(userId, provider, providerUserId, now);
    }
}
