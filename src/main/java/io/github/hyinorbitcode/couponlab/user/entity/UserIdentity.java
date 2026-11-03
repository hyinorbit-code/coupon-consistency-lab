package io.github.hyinorbitcode.couponlab.user.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "user_identities")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserIdentity {

    public static final int MAX_PROVIDER_USER_ID_LENGTH = 64;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    private IdentityProvider provider;

    private String providerUserId;

    private Instant linkedAt;

    private UserIdentity(User user, IdentityProvider provider, String providerUserId, Instant linkedAt) {
        this.user = user;
        this.provider = provider;
        this.providerUserId = providerUserId;
        this.linkedAt = linkedAt;
    }

    public static UserIdentity link(User user, IdentityProvider provider, String providerUserId, Instant now) {
        Objects.requireNonNull(user, "user");
        Objects.requireNonNull(provider, "provider");
        Objects.requireNonNull(now, "now");
        requireProviderUserId(providerUserId);
        return new UserIdentity(user, provider, providerUserId, now);
    }

    private static void requireProviderUserId(String providerUserId) {
        if (providerUserId == null || providerUserId.isBlank()) {
            throw new IllegalArgumentException("providerUserId must not be blank");
        }
        if (providerUserId.length() > MAX_PROVIDER_USER_ID_LENGTH) {
            throw new IllegalArgumentException(
                    "providerUserId must be at most " + MAX_PROVIDER_USER_ID_LENGTH + " characters");
        }
    }
}
