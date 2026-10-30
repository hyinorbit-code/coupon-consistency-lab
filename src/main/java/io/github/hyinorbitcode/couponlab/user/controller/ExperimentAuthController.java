package io.github.hyinorbitcode.couponlab.user.controller;

import io.github.hyinorbitcode.couponlab.user.dto.request.ExperimentTokenRequest;
import io.github.hyinorbitcode.couponlab.user.dto.response.TokenResponse;
import io.github.hyinorbitcode.couponlab.user.entity.IdentityProvider;
import io.github.hyinorbitcode.couponlab.user.entity.User;
import io.github.hyinorbitcode.couponlab.user.security.JwtTokenIssuer;
import io.github.hyinorbitcode.couponlab.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
class ExperimentAuthController {

    private final UserService userService;
    private final JwtTokenIssuer jwtTokenIssuer;

    @PostMapping("/token")
    TokenResponse issueToken(@Valid @RequestBody ExperimentTokenRequest request) {
        User user = userService.findOrCreate(
                IdentityProvider.EXPERIMENT, request.providerUserId(), request.nickname(), request.role());
        JwtTokenIssuer.IssuedToken token = jwtTokenIssuer.issue(user);
        return new TokenResponse(token.accessToken(), token.expiresAt());
    }
}
