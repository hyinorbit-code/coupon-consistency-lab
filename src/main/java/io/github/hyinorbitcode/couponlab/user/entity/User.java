package io.github.hyinorbitcode.couponlab.user.entity;

import io.github.hyinorbitcode.couponlab.user.Role;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 로그인 주체. 이메일 가입자는 이메일·비밀번호 해시를 갖고, 카카오·실험용 계정으로만 가입한 사용자는 둘 다 없다.
 * 외부 계정은 {@link UserIdentity}로 연동한다(ADR-0010).
 */
@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    public static final int MAX_NICKNAME_LENGTH = 50;
    public static final int MAX_EMAIL_LENGTH = 254;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;

    private String passwordHash;

    private String nickname;

    @Enumerated(EnumType.STRING)
    private Role role;

    @Enumerated(EnumType.STRING)
    private UserStatus status;

    private Instant emailVerifiedAt;

    private Instant createdAt;

    private User(String nickname, Role role, UserStatus status, Instant createdAt) {
        this.nickname = nickname;
        this.role = role;
        this.status = status;
        this.createdAt = createdAt;
    }

    /** 이메일 없이 외부 계정(카카오, 실험용)으로 가입하는 사용자. 바로 활성 상태다. */
    public static User createWithoutCredentials(String nickname, Role role, Instant now) {
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(now, "now");
        requireNickname(nickname);
        return new User(nickname, role, UserStatus.ACTIVE, now);
    }

    /**
     * 이메일로 가입하는 사용자. 이메일 인증 전까지 로그인할 수 없다(ADR-0010).
     * 이메일은 {@link #normalizeEmail(String)}로 정규화된 값이어야 하고, 비밀번호는 해시만 받는다.
     */
    public static User signUpWithEmail(String normalizedEmail, String passwordHash, String nickname, Instant now) {
        Objects.requireNonNull(now, "now");
        requireNormalizedEmail(normalizedEmail);
        requireText(passwordHash, "passwordHash");
        requireNickname(nickname);
        User user = new User(nickname, Role.USER, UserStatus.PENDING_VERIFICATION, now);
        user.email = normalizedEmail;
        user.passwordHash = passwordHash;
        return user;
    }

    /** 이메일은 앞뒤 공백을 지우고 소문자로 저장한다. User@Example.com과 user@example.com은 같은 계정이다. */
    public static String normalizeEmail(String email) {
        return email == null ? null : email.strip().toLowerCase(Locale.ROOT);
    }

    /** 이메일 인증을 마치고 활성화한다. 이미 활성인 사용자에게는 아무 일도 하지 않는다. */
    public void verifyEmail(Instant now) {
        if (status != UserStatus.PENDING_VERIFICATION) {
            return;
        }
        this.status = UserStatus.ACTIVE;
        this.emailVerifiedAt = Objects.requireNonNull(now, "now");
    }

    public boolean isPendingVerification() {
        return status == UserStatus.PENDING_VERIFICATION;
    }

    /** 서비스가 정규화해서 넘겨야 한다. 어기면 사용자 입력 오류가 아니라 코드 오류다. */
    private static void requireNormalizedEmail(String email) {
        requireText(email, "email");
        if (email.length() > MAX_EMAIL_LENGTH) {
            throw new IllegalArgumentException("email must be at most " + MAX_EMAIL_LENGTH + " characters");
        }
        if (!email.equals(normalizeEmail(email))) {
            throw new IllegalArgumentException("email must be normalized before creating a user");
        }
    }

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }

    private static void requireNickname(String nickname) {
        requireText(nickname, "nickname");
        if (nickname.length() > MAX_NICKNAME_LENGTH) {
            throw new IllegalArgumentException("nickname must be at most " + MAX_NICKNAME_LENGTH + " characters");
        }
    }
}
