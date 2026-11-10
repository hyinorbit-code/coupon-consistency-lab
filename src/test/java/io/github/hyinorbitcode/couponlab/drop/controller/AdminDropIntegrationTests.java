package io.github.hyinorbitcode.couponlab.drop.controller;

import io.github.hyinorbitcode.couponlab.DatabaseCleaner;
import io.github.hyinorbitcode.couponlab.IntegrationTest;
import io.github.hyinorbitcode.couponlab.drop.entity.DropEvent;
import io.github.hyinorbitcode.couponlab.drop.repository.DropEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.json.JsonMapper;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class AdminDropIntegrationTests {

    private static final String DROPS_URL = "/api/admin/drops";
    private static final String TOKEN_URL = "/api/experiment/auth/token";

    private static final String OPEN_AT = "2099-01-01T00:00:00Z";
    private static final String CLOSE_AT = "2099-01-01T01:00:00Z";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JsonMapper jsonMapper;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    DropEventRepository dropEventRepository;

    @BeforeEach
    void setUp() {
        DatabaseCleaner.truncateAll(jdbcTemplate);
    }

    @Test
    @DisplayName("관리자가 이벤트를 만들면 201과 초안 상태의 이벤트를 준다")
    void createsDraft() throws Exception {
        String admin = issueToken("ADMIN");

        createDrop(admin, body("커피 드롭", "아메리카노 쿠폰", OPEN_AT, CLOSE_AT))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.title").value("커피 드롭"))
                .andExpect(jsonPath("$.prizeDescription").value("아메리카노 쿠폰"))
                .andExpect(jsonPath("$.openAt").value(OPEN_AT))
                .andExpect(jsonPath("$.closeAt").value(CLOSE_AT))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.phase").value("UPCOMING"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());

        assertThat(dropEventRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("만든 이벤트를 ID로 조회하면 같은 내용을 준다")
    void getsCreatedDrop() throws Exception {
        String admin = issueToken("ADMIN");
        String json = createDrop(admin, body("커피 드롭", "아메리카노 쿠폰", OPEN_AT, CLOSE_AT))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long id = jsonMapper.readTree(json).get("id").asLong();

        mockMvc.perform(get(DROPS_URL + "/" + id).header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.title").value("커피 드롭"))
                .andExpect(jsonPath("$.prizeDescription").value("아메리카노 쿠폰"))
                .andExpect(jsonPath("$.openAt").value(OPEN_AT))
                .andExpect(jsonPath("$.closeAt").value(CLOSE_AT))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.phase").value("UPCOMING"));
    }

    @Test
    @DisplayName("없는 이벤트를 조회하면 404를 받는다")
    void returnsNotFound() throws Exception {
        String admin = issueToken("ADMIN");

        mockMvc.perform(get(DROPS_URL + "/999999").header("Authorization", "Bearer " + admin))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("DROP_NOT_FOUND"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidRequests")
    @DisplayName("입력이 잘못되면 400과 잘못된 필드를 알려 준다")
    void rejectsInvalidRequest(String field, String requestBody) throws Exception {
        String admin = issueToken("ADMIN");

        createDrop(admin, requestBody)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.errors[0].field").value(field));

        assertThat(dropEventRepository.count()).isZero();
    }

    static Stream<Arguments> invalidRequests() {
        return Stream.of(
                Arguments.of("title", body(" ", "아메리카노 쿠폰", OPEN_AT, CLOSE_AT)),
                Arguments.of("title", body("가".repeat(DropEvent.MAX_TITLE_LENGTH + 1), "아메리카노 쿠폰", OPEN_AT, CLOSE_AT)),
                Arguments.of("prizeDescription", body("커피 드롭", "", OPEN_AT, CLOSE_AT)),
                Arguments.of("prizeDescription", body("커피 드롭", "가".repeat(DropEvent.MAX_PRIZE_DESCRIPTION_LENGTH + 1), OPEN_AT, CLOSE_AT)),
                Arguments.of("openAt", """
                        {"title":"커피 드롭","prizeDescription":"아메리카노 쿠폰","closeAt":"%s"}
                        """.formatted(CLOSE_AT)),
                Arguments.of("closeAt", """
                        {"title":"커피 드롭","prizeDescription":"아메리카노 쿠폰","openAt":"%s"}
                        """.formatted(OPEN_AT))
        );
    }

    @Test
    @DisplayName("오픈 시각이 종료 시각보다 늦으면 400을 받는다")
    void rejectsReversedPeriod() throws Exception {
        String admin = issueToken("ADMIN");

        createDrop(admin, body("커피 드롭", "아메리카노 쿠폰", CLOSE_AT, OPEN_AT))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));

        assertThat(dropEventRepository.count()).isZero();
    }

    @Test
    @DisplayName("일반 사용자가 이벤트를 만들면 403을 받는다")
    void rejectsUser() throws Exception {
        String user = issueToken("USER");

        createDrop(user, body("커피 드롭", "아메리카노 쿠폰", OPEN_AT, CLOSE_AT))
                .andExpect(status().isForbidden());

        assertThat(dropEventRepository.count()).isZero();
    }

    @Test
    @DisplayName("토큰 없이 이벤트를 만들면 401을 받는다")
    void rejectsWithoutToken() throws Exception {
        mockMvc.perform(post(DROPS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("커피 드롭", "아메리카노 쿠폰", OPEN_AT, CLOSE_AT)))
                .andExpect(status().isUnauthorized());

        assertThat(dropEventRepository.count()).isZero();
    }

    private String issueToken(String role) throws Exception {
        String name = role.toLowerCase();
        String json = mockMvc.perform(post(TOKEN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"providerUserId":"%s-1","nickname":"%s","role":"%s"}
                                """.formatted(name, name, role)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return jsonMapper.readTree(json).get("accessToken").asString();
    }

    private ResultActions createDrop(String token, String requestBody) throws Exception {
        return mockMvc.perform(post(DROPS_URL)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody));
    }

    private static String body(String title, String prizeDescription, String openAt, String closeAt) {
        return """
                {"title":"%s","prizeDescription":"%s","openAt":"%s","closeAt":"%s"}
                """.formatted(title, prizeDescription, openAt, closeAt);
    }
}
