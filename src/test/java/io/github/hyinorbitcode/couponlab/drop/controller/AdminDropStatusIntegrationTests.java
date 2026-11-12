package io.github.hyinorbitcode.couponlab.drop.controller;

import io.github.hyinorbitcode.couponlab.DatabaseCleaner;
import io.github.hyinorbitcode.couponlab.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class AdminDropStatusIntegrationTests {

    private static final String TOKEN_URL = "/api/experiment/auth/token";
    private static final String DROPS_URL = "/api/admin/drops";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JsonMapper jsonMapper;

    @Autowired
    JdbcTemplate jdbcTemplate;

    private String admin;

    @BeforeEach
    void setUp() throws Exception {
        DatabaseCleaner.truncateAll(jdbcTemplate);
        admin = issueToken("ADMIN");
    }

    @Test
    @DisplayName("코드를 등록한 이벤트를 공개하면 200과 공개 상태를 준다")
    void publishesDropWithCodes() throws Exception {
        long dropId = createDropWithCodes();

        action(admin, dropId, "publish")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(dropId))
                .andExpect(jsonPath("$.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.phase").value("UPCOMING"));

        assertThat(statusOf(dropId)).isEqualTo("PUBLISHED");
    }

    @Test
    @DisplayName("코드 없이 공개하면 409 NO_CODES를 받고 초안으로 남는다")
    void rejectsPublishingWithoutCodes() throws Exception {
        long dropId = createDrop();

        action(admin, dropId, "publish")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("NO_CODES"));

        assertThat(statusOf(dropId)).isEqualTo("DRAFT");
    }

    @Test
    @DisplayName("이미 공개한 이벤트를 다시 공개하면 409를 받는다")
    void rejectsPublishingTwice() throws Exception {
        long dropId = createDropWithCodes();
        action(admin, dropId, "publish").andExpect(status().isOk());

        action(admin, dropId, "publish")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_DROP_STATUS"));
    }

    @Test
    @DisplayName("취소한 이벤트는 공개할 수 없다")
    void rejectsPublishingCanceled() throws Exception {
        long dropId = createDropWithCodes();
        action(admin, dropId, "cancel").andExpect(status().isOk());

        action(admin, dropId, "publish")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_DROP_STATUS"));

        assertThat(statusOf(dropId)).isEqualTo("CANCELED");
    }

    @Test
    @DisplayName("초안 이벤트를 취소하면 200과 취소 상태를 준다")
    void cancelsDraft() throws Exception {
        long dropId = createDrop();

        action(admin, dropId, "cancel")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELED"));

        assertThat(statusOf(dropId)).isEqualTo("CANCELED");
    }

    @Test
    @DisplayName("공개한 이벤트도 오픈 전이면 취소할 수 있다")
    void cancelsPublishedBeforeOpen() throws Exception {
        long dropId = createDropWithCodes();
        action(admin, dropId, "publish").andExpect(status().isOk());

        action(admin, dropId, "cancel")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELED"));
    }

    @Test
    @DisplayName("이미 취소한 이벤트를 다시 취소하면 409를 받는다")
    void rejectsCancelingTwice() throws Exception {
        long dropId = createDrop();
        action(admin, dropId, "cancel").andExpect(status().isOk());

        action(admin, dropId, "cancel")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_DROP_STATUS"));
    }

    @Test
    @DisplayName("오픈 시각이 지난 이벤트는 공개하거나 취소할 수 없다")
    void rejectsStatusChangeAfterOpen() throws Exception {
        long dropId = createDropWithCodes();
        moveOpenToPast(dropId);

        action(admin, dropId, "publish")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_DROP_STATUS"));
        action(admin, dropId, "cancel")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_DROP_STATUS"));

        assertThat(statusOf(dropId)).isEqualTo("DRAFT");
    }

    @Test
    @DisplayName("공개 후 조회하면 공개 상태가 유지된다")
    void publishedStatusPersists() throws Exception {
        long dropId = createDropWithCodes();
        action(admin, dropId, "publish").andExpect(status().isOk());

        mockMvc.perform(get(DROPS_URL + "/" + dropId).header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));
    }

    @Test
    @DisplayName("없는 이벤트를 공개하거나 취소하면 404를 받는다")
    void rejectsUnknownDrop() throws Exception {
        action(admin, 999999L, "publish")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("DROP_NOT_FOUND"));
        action(admin, 999999L, "cancel")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("DROP_NOT_FOUND"));
    }

    @Test
    @DisplayName("일반 사용자가 공개하거나 취소하면 403을 받는다")
    void rejectsUser() throws Exception {
        long dropId = createDropWithCodes();
        String user = issueToken("USER");

        action(user, dropId, "publish").andExpect(status().isForbidden());
        action(user, dropId, "cancel").andExpect(status().isForbidden());

        assertThat(statusOf(dropId)).isEqualTo("DRAFT");
    }

    @Test
    @DisplayName("토큰 없이 공개하면 401을 받는다")
    void rejectsWithoutToken() throws Exception {
        long dropId = createDropWithCodes();

        mockMvc.perform(post(DROPS_URL + "/" + dropId + "/publish"))
                .andExpect(status().isUnauthorized());

        assertThat(statusOf(dropId)).isEqualTo("DRAFT");
    }

    private ResultActions action(String token, long dropId, String name) throws Exception {
        return mockMvc.perform(post(DROPS_URL + "/" + dropId + "/" + name)
                .header("Authorization", "Bearer " + token));
    }

    private long createDrop() throws Exception {
        String json = mockMvc.perform(post(DROPS_URL)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"커피 드롭","prizeDescription":"아메리카노 쿠폰",
                                 "openAt":"2099-01-01T00:00:00Z","closeAt":"2099-01-01T01:00:00Z"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return jsonMapper.readTree(json).get("id").asLong();
    }

    private long createDropWithCodes() throws Exception {
        long dropId = createDrop();
        mockMvc.perform(post(DROPS_URL + "/" + dropId + "/codes")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"codes":["COFFEE-%d-1","COFFEE-%d-2"]}
                                """.formatted(dropId, dropId)))
                .andExpect(status().isOk());
        return dropId;
    }

    private void moveOpenToPast(long dropId) {
        jdbcTemplate.update(
                "UPDATE drop_events SET open_at = '2000-01-01 00:00:00', close_at = '2099-01-01 00:00:00' WHERE id = ?",
                dropId);
    }

    private String statusOf(long dropId) {
        return jdbcTemplate.queryForObject("SELECT status FROM drop_events WHERE id = ?", String.class, dropId);
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
}
