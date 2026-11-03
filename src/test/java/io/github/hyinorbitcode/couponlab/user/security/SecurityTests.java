package io.github.hyinorbitcode.couponlab.user.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import io.github.hyinorbitcode.couponlab.IntegrationTest;
import io.github.hyinorbitcode.couponlab.user.Role;
import io.github.hyinorbitcode.couponlab.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@IntegrationTest
@Import(SecurityTests.SecurityTestController.class)
class SecurityTests {

    private static final String ME = "/api/security-test/me";
    private static final String ADMIN = "/api/admin/security-test";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JwtTokenIssuer jwtTokenIssuer;

    @Autowired
    JwtEncoder jwtEncoder;

    @Autowired
    AuthProperties authProperties;

    @Test
    @DisplayName("토큰 없이 호출하면 401과 UNAUTHORIZED 코드를 받는다")
    void rejectsMissingToken() throws Exception {
        mockMvc.perform(get(ME))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(header().exists("WWW-Authenticate"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.instance").value(ME));
    }

    @Test
    @DisplayName("발급한 토큰으로 호출하면 사용자 ID와 역할 권한이 채워진다")
    void acceptsIssuedToken() throws Exception {
        String token = jwtTokenIssuer.issue(user(42L, Role.USER)).accessToken();

        mockMvc.perform(get(ME).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subject").value("42"))
                .andExpect(jsonPath("$.authorities", hasItem("ROLE_USER")));
    }

    @Test
    @DisplayName("서명이 다른 토큰은 401을 받는다")
    void rejectsTokenWithWrongSignature() throws Exception {
        byte[] otherSecret = "a".repeat(32).getBytes(StandardCharsets.UTF_8);
        JwtEncoder otherEncoder = new NimbusJwtEncoder(new ImmutableSecret<>(new SecretKeySpec(otherSecret, "HmacSHA256")));
        JwtTokenIssuer otherIssuer = new JwtTokenIssuer(otherEncoder, authProperties, Clock.systemUTC());
        String token = otherIssuer.issue(user(42L, Role.USER)).accessToken();

        mockMvc.perform(get(ME).header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("발급자가 다른 토큰은 401을 받는다")
    void rejectsTokenWithWrongIssuer() throws Exception {
        AuthProperties otherIssuerProperties =
                new AuthProperties(authProperties.jwtSecret(), "a".repeat(10), authProperties.tokenTtl());
        JwtTokenIssuer otherIssuer = new JwtTokenIssuer(jwtEncoder, otherIssuerProperties, Clock.systemUTC());
        String token = otherIssuer.issue(user(42L, Role.USER)).accessToken();

        mockMvc.perform(get(ME).header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("만료된 토큰은 401을 받는다")
    void rejectsExpiredToken() throws Exception {
        Instant issuedAt = Instant.now().minus(authProperties.tokenTtl()).minus(Duration.ofHours(1));
        JwtTokenIssuer pastIssuer = new JwtTokenIssuer(jwtEncoder, authProperties, Clock.fixed(issuedAt, ZoneOffset.UTC));
        String token = pastIssuer.issue(user(42L, Role.USER)).accessToken();

        mockMvc.perform(get(ME).header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("USER 토큰으로 관리자 API를 호출하면 403과 FORBIDDEN 코드를 받는다")
    void forbidsUserOnAdminApi() throws Exception {
        String token = jwtTokenIssuer.issue(user(42L, Role.USER)).accessToken();

        mockMvc.perform(get(ADMIN).header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("ADMIN 토큰으로 관리자 API를 호출할 수 있다")
    void allowsAdminOnAdminApi() throws Exception {
        String token = jwtTokenIssuer.issue(user(1L, Role.ADMIN)).accessToken();

        mockMvc.perform(get(ADMIN).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("저장 전 사용자에게는 토큰을 발급하지 않는다")
    void rejectsUnsavedUser() {
        User unsaved = User.createWithoutCredentials("tester", Role.USER, Instant.now());

        assertThatThrownBy(() -> jwtTokenIssuer.issue(unsaved))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("saved");
    }

    private static User user(Long id, Role role) {
        User user = User.createWithoutCredentials("tester", role, Instant.now());
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    @RestController
    static class SecurityTestController {

        @GetMapping("/security-test/me")
        Map<String, Object> me(@AuthenticationPrincipal Jwt jwt, JwtAuthenticationToken authentication) {
            List<String> authorities = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .toList();
            return Map.of("subject", jwt.getSubject(), "authorities", authorities);
        }

        @GetMapping("/admin/security-test")
        String admin() {
            return "ok";
        }
    }
}
