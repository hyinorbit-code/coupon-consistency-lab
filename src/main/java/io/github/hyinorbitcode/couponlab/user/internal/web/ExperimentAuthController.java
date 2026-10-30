package io.github.hyinorbitcode.couponlab.user.internal.web;

import io.github.hyinorbitcode.couponlab.user.Role;
import io.github.hyinorbitcode.couponlab.user.internal.application.UserService;
import io.github.hyinorbitcode.couponlab.user.internal.domain.AuthProvider;
import io.github.hyinorbitcode.couponlab.user.internal.domain.User;
import io.github.hyinorbitcode.couponlab.user.internal.security.JwtTokenIssuer;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 실험용 토큰 발급. experiment 프로필에서만 등록되므로 실서비스에는 이 엔드포인트가 없다(ADR-0003, ADR-0007).
 * 카카오 첫 로그인처럼 사용자가 없으면 만들고, 있으면 기존 사용자에게 토큰만 발급한다.
 */
@Profile("experiment")
@RestController
@RequestMapping("/experiment/auth")
class ExperimentAuthController {

    private final UserService userService;
    private final JwtTokenIssuer jwtTokenIssuer;

    ExperimentAuthController(UserService userService, JwtTokenIssuer jwtTokenIssuer) {
        this.userService = userService;
        this.jwtTokenIssuer = jwtTokenIssuer;
    }

    @PostMapping("/token")
    TokenResponse issueToken(@Valid @RequestBody TokenRequest request) {
        User user = userService.findOrCreate(
                AuthProvider.EXPERIMENT, request.providerUserId(), request.nickname(), request.role());
        JwtTokenIssuer.IssuedToken token = jwtTokenIssuer.issue(user);
        return new TokenResponse(token.accessToken(), token.expiresAt());
    }

    record TokenRequest(
            @NotBlank @Size(max = User.MAX_PROVIDER_USER_ID_LENGTH) String providerUserId,
            @NotBlank @Size(max = User.MAX_NICKNAME_LENGTH) String nickname,
            @NotNull Role role) {
    }

    record TokenResponse(String accessToken, Instant expiresAt) {
    }
}
