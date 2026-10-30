package io.github.hyinorbitcode.couponlab.user.internal;

import io.github.hyinorbitcode.couponlab.user.Role;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "users")
class User {

    static final int MAX_PROVIDER_USER_ID_LENGTH = 64;
    static final int MAX_NICKNAME_LENGTH = 50;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private AuthProvider authProvider;

    private String providerUserId;

    private String nickname;

    @Enumerated(EnumType.STRING)
    private Role role;

    private Instant createdAt;

    protected User() {
    }

    private User(AuthProvider authProvider, String providerUserId, String nickname, Role role, Instant createdAt) {
        this.authProvider = authProvider;
        this.providerUserId = providerUserId;
        this.nickname = nickname;
        this.role = role;
        this.createdAt = createdAt;
    }

    static User create(AuthProvider authProvider, String providerUserId, String nickname, Role role, Instant now) {
        Objects.requireNonNull(authProvider, "authProvider");
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(now, "now");
        requireText(providerUserId, "providerUserId", MAX_PROVIDER_USER_ID_LENGTH);
        requireText(nickname, "nickname", MAX_NICKNAME_LENGTH);
        return new User(authProvider, providerUserId, nickname, role, now);
    }

    private static void requireText(String value, String name, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        if (value.length() > maxLength) {
            throw new IllegalArgumentException(name + " must be at most " + maxLength + " characters");
        }
    }

    Long getId() {
        return id;
    }

    String getNickname() {
        return nickname;
    }

    Role getRole() {
        return role;
    }
}
