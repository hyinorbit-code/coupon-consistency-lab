package io.github.hyinorbitcode.couponlab.user.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import io.github.hyinorbitcode.couponlab.DatabaseCleaner;
import io.github.hyinorbitcode.couponlab.IntegrationTest;
import io.github.hyinorbitcode.couponlab.user.security.AuthProperties;
import io.github.hyinorbitcode.couponlab.user.security.JwtTokenIssuer;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.web.servlet.MockMvc;

@IntegrationTest
class ExperimentAuthIntegrationTests {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    JwtEncoder jwtEncoder;

    @Autowired
    AuthProperties authProperties;

    @BeforeEach
    void setUp() {
        DatabaseCleaner.truncateAll(jdbcTemplate);
    }

    @Test
    @DisplayName("발급받은 토큰으로 내 정보를 조회한다")
    void issuesTokenAndReadsMe() throws Exception {
        String token = issueToken("user-1", "닉네임", "USER");

        mockMvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value("닉네임"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    @DisplayName("같은 식별자로 다시 발급받으면 같은 사용자이고 닉네임·역할은 바뀌지 않는다")
    void reissuesTokenForExistingUser() throws Exception {
        long firstId = readMyId(issueToken("user-1", "처음", "USER"));
        String secondToken = issueToken("user-1", "바뀐닉네임", "ADMIN");

        mockMvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, bearer(secondToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(firstId))
                .andExpect(jsonPath("$.nickname").value("처음"))
                .andExpect(jsonPath("$.role").value("USER"));
        assertThat(countUsers()).isEqualTo(1);
    }

    @Test
    @DisplayName("토큰 없이 요청하면 401을 받는다")
    void rejectsMissingToken() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("변조된 토큰은 401을 받는다")
    void rejectsTamperedToken() throws Exception {
        String token = issueToken("user-1", "닉네임", "USER");
        String tampered = token.substring(0, token.length() - 2) + (token.endsWith("A") ? "BB" : "AA");

        mockMvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, bearer(tampered)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("만료된 토큰은 401을 받는다")
    void rejectsExpiredToken() throws Exception {
        long userId = readMyId(issueToken("user-1", "닉네임", "USER"));
        Instant issuedAt = Instant.now().minus(3, ChronoUnit.HOURS);
        String expired = encode(userId, issuedAt, issuedAt.plus(1, ChronoUnit.HOURS));

        mockMvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, bearer(expired)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("필수 값이 빠진 발급 요청은 400과 ProblemDetail을 받는다")
    void rejectsInvalidTokenRequest() throws Exception {
        mockMvc.perform(post("/api/experiment/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"providerUserId": "", "nickname": "닉네임", "role": "USER"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    private String issueToken(String providerUserId, String nickname, String role) throws Exception {
        String body = mockMvc.perform(post("/api/experiment/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"providerUserId": "%s", "nickname": "%s", "role": "%s"}
                                """.formatted(providerUserId, nickname, role)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expiresAt").exists())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.accessToken");
    }

    private long readMyId(String token) throws Exception {
        String body = mockMvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private String encode(long userId, Instant issuedAt, Instant expiresAt) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(authProperties.issuer())
                .subject(String.valueOf(userId))
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim(JwtTokenIssuer.ROLE_CLAIM, "USER")
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    private Integer countUsers() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Integer.class);
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
