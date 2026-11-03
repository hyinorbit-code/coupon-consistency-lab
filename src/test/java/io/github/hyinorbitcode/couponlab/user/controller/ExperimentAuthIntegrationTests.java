package io.github.hyinorbitcode.couponlab.user.controller;

import io.github.hyinorbitcode.couponlab.DatabaseCleaner;
import io.github.hyinorbitcode.couponlab.IntegrationTest;
import io.github.hyinorbitcode.couponlab.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@IntegrationTest
class ExperimentAuthIntegrationTests {

    private static final String TOKEN_URL = "/api/experiment/auth/token";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JwtDecoder jwtDecoder;

    @Autowired
    JsonMapper jsonMapper;

    @Autowired
    UserRepository userRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        DatabaseCleaner.truncateAll(jdbcTemplate);
    }

    @Test
    @DisplayName("처음 요청하면 사용자를 만들고 토큰과 만료 시각을 준다")
    void issuesTokenForNewUser() throws Exception {
        requestToken("u-1", "tester", "USER")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.expiresAt").isNotEmpty());

        assertThat(userRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("발급한 토큰에는 사용자 ID와 요청한 역할이 담긴다")
    void tokenCarriesUserIdAndRole() throws Exception {
        Jwt jwt = decode(requestToken("admin-1", "admin", "ADMIN"));

        Long userId = userRepository.findAll().getFirst().getId();
        assertThat(jwt.getSubject()).isEqualTo(String.valueOf(userId));
        assertThat(jwt.getClaimAsString("role")).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("같은 식별자로 다시 요청하면 같은 사용자의 토큰을 주고 새로 만들지 않는다")
    void reusesExistingUser() throws Exception {
        Jwt first = decode(requestToken("u-1", "tester", "USER"));

        Jwt second = decode(requestToken("u-1", "changed", "ADMIN"));

        assertThat(second.getSubject()).isEqualTo(first.getSubject());
        assertThat(second.getClaimAsString("role")).isEqualTo("USER");
        assertThat(userRepository.count()).isEqualTo(1);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidRequests")
    @DisplayName("입력이 잘못되면 400과 잘못된 필드를 알려 준다")
    void rejectsInvalidRequest(String field, String body) throws Exception {
        mockMvc.perform(post(TOKEN_URL).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.errors[0].field").value(field));

        assertThat(userRepository.count()).isZero();
    }

    static Stream<Arguments> invalidRequests() {
        return Stream.of(
                Arguments.of("providerUserId", body("", "tester", "USER")),
                Arguments.of("providerUserId", body("1".repeat(65), "tester", "USER")),
                Arguments.of("nickname", body("u-1", " ", "USER")),
                Arguments.of("nickname", body("u-1", "가".repeat(51), "USER")),
                Arguments.of("role", """
                        {"providerUserId":"u-1","nickname":"tester"}
                        """)
        );
    }

    @Test
    @DisplayName("없는 역할을 보내면 400을 받는다")
    void rejectsUnknownRole() throws Exception {
        requestToken("u-1", "tester", "SUPERUSER")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));

        assertThat(userRepository.count()).isZero();
    }

    private ResultActions requestToken(String providerUserId, String nickname, String role) throws Exception {
        return mockMvc.perform(post(TOKEN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(providerUserId, nickname, role)));
    }

    private Jwt decode(ResultActions result) throws Exception {
        String json = result.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode response = jsonMapper.readTree(json);
        return jwtDecoder.decode(response.get("accessToken").asString());
    }

    private static String body(String providerUserId, String nickname, String role) {
        return """
                {"providerUserId":"%s","nickname":"%s","role":"%s"}
                """.formatted(providerUserId, nickname, role);
    }
}
