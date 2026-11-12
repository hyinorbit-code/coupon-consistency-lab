package io.github.hyinorbitcode.couponlab.participation.controller;

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
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class EntryIntegrationTests {

    private static final String TOKEN_URL = "/api/experiment/auth/token";
    private static final String ADMIN_DROPS_URL = "/api/admin/drops";

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
        admin = issueToken("admin-1", "ADMIN");
    }

    @Test
    @DisplayName("오픈된 이벤트에 응모하면 201과 복호화된 경품 코드를 준다")
    void winsPrize() throws Exception {
        long dropId = openDropWithCodes("COFFEE-0001", "COFFEE-0002");
        String user = issueToken("user-1", "USER");

        enter(user, dropId)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.result").value("WON"))
                .andExpect(jsonPath("$.prize.id").isNumber())
                .andExpect(jsonPath("$.prize.dropId").value(dropId))
                .andExpect(jsonPath("$.prize.dropTitle").value("커피 드롭"))
                .andExpect(jsonPath("$.prize.prizeDescription").value("아메리카노 쿠폰"))
                .andExpect(jsonPath("$.prize.code").value("COFFEE-0001"))
                .andExpect(jsonPath("$.prize.wonAt").isNotEmpty());

        assertThat(assignedCount(dropId)).isEqualTo(1);
    }

    @Test
    @DisplayName("이미 당첨된 사용자가 다시 응모하면 200과 같은 경품을 주고 새로 할당하지 않는다")
    void returnsSamePrizeOnRetry() throws Exception {
        long dropId = openDropWithCodes("COFFEE-0001", "COFFEE-0002");
        String user = issueToken("user-1", "USER");
        JsonNode first = body(enter(user, dropId).andExpect(status().isCreated()));

        JsonNode second = body(enter(user, dropId).andExpect(status().isOk()));

        assertThat(second.get("prize").get("id").asLong()).isEqualTo(first.get("prize").get("id").asLong());
        assertThat(second.get("prize").get("code").asString()).isEqualTo(first.get("prize").get("code").asString());
        assertThat(assignedCount(dropId)).isEqualTo(1);
    }

    @Test
    @DisplayName("서로 다른 사용자는 서로 다른 코드를 받는다")
    void differentUsersGetDifferentCodes() throws Exception {
        long dropId = openDropWithCodes("COFFEE-0001", "COFFEE-0002");

        String first = body(enter(issueToken("user-1", "USER"), dropId).andExpect(status().isCreated()))
                .get("prize").get("code").asString();
        String second = body(enter(issueToken("user-2", "USER"), dropId).andExpect(status().isCreated()))
                .get("prize").get("code").asString();

        assertThat(List.of(first, second)).containsExactlyInAnyOrder("COFFEE-0001", "COFFEE-0002");
        assertThat(assignedCount(dropId)).isEqualTo(2);
    }

    @Test
    @DisplayName("남은 코드가 없으면 409 SOLD_OUT을 받고 할당은 늘지 않는다")
    void soldOut() throws Exception {
        long dropId = openDropWithCodes("COFFEE-0001");
        enter(issueToken("user-1", "USER"), dropId).andExpect(status().isCreated());

        enter(issueToken("user-2", "USER"), dropId)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SOLD_OUT"));

        assertThat(assignedCount(dropId)).isEqualTo(1);
    }

    @Test
    @DisplayName("오픈 전에 응모하면 409 DROP_NOT_OPEN을 받고 아무것도 할당하지 않는다")
    void rejectsBeforeOpen() throws Exception {
        long dropId = publishedDropWithCodes("COFFEE-0001");

        enter(issueToken("user-1", "USER"), dropId)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DROP_NOT_OPEN"));

        assertThat(assignedCount(dropId)).isZero();
    }

    @Test
    @DisplayName("종료 후에 응모하면 409 DROP_CLOSED를 받고 아무것도 할당하지 않는다")
    void rejectsAfterClose() throws Exception {
        long dropId = publishedDropWithCodes("COFFEE-0001");
        movePeriod(dropId, "2000-01-01 00:00:00", "2000-01-01 01:00:00");

        enter(issueToken("user-1", "USER"), dropId)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DROP_CLOSED"));

        assertThat(assignedCount(dropId)).isZero();
    }

    @Test
    @DisplayName("종료 후에도 이미 당첨된 사용자는 200으로 자기 경품을 받는다")
    void returnsPrizeAfterClose() throws Exception {
        long dropId = openDropWithCodes("COFFEE-0001");
        String user = issueToken("user-1", "USER");
        enter(user, dropId).andExpect(status().isCreated());
        movePeriod(dropId, "2000-01-01 00:00:00", "2000-01-01 01:00:00");

        enter(user, dropId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prize.code").value("COFFEE-0001"));
    }

    @Test
    @DisplayName("공개하지 않은 이벤트에 응모하면 404를 받는다")
    void rejectsDraftDrop() throws Exception {
        long dropId = createDrop();
        registerCodes(dropId, "COFFEE-0001");
        movePeriod(dropId, "2000-01-01 00:00:00", "2099-01-01 00:00:00");

        enter(issueToken("user-1", "USER"), dropId)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("DROP_NOT_FOUND"));

        assertThat(assignedCount(dropId)).isZero();
    }

    @Test
    @DisplayName("취소한 이벤트에 응모하면 404를 받는다")
    void rejectsCanceledDrop() throws Exception {
        long dropId = publishedDropWithCodes("COFFEE-0001");
        mockMvc.perform(post(ADMIN_DROPS_URL + "/" + dropId + "/cancel").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk());

        enter(issueToken("user-1", "USER"), dropId)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("DROP_NOT_FOUND"));
    }

    @Test
    @DisplayName("없는 이벤트에 응모하면 404를 받는다")
    void rejectsUnknownDrop() throws Exception {
        enter(issueToken("user-1", "USER"), 999999L)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("DROP_NOT_FOUND"));
    }

    @Test
    @DisplayName("토큰 없이 응모하면 401을 받는다")
    void rejectsWithoutToken() throws Exception {
        long dropId = openDropWithCodes("COFFEE-0001");

        mockMvc.perform(post("/api/drops/" + dropId + "/entries"))
                .andExpect(status().isUnauthorized());

        assertThat(assignedCount(dropId)).isZero();
    }

    private ResultActions enter(String token, long dropId) throws Exception {
        return mockMvc.perform(post("/api/drops/" + dropId + "/entries")
                .header("Authorization", "Bearer " + token));
    }

    private JsonNode body(ResultActions result) throws Exception {
        return jsonMapper.readTree(result.andReturn().getResponse().getContentAsString());
    }

    /** 코드를 등록하고 공개한 뒤 오픈 시각을 과거로 옮겨 진행 중인 이벤트를 만든다. */
    private long openDropWithCodes(String... codes) throws Exception {
        long dropId = publishedDropWithCodes(codes);
        movePeriod(dropId, "2000-01-01 00:00:00", "2099-01-01 00:00:00");
        return dropId;
    }

    private long publishedDropWithCodes(String... codes) throws Exception {
        long dropId = createDrop();
        registerCodes(dropId, codes);
        mockMvc.perform(post(ADMIN_DROPS_URL + "/" + dropId + "/publish").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk());
        return dropId;
    }

    private long createDrop() throws Exception {
        String json = mockMvc.perform(post(ADMIN_DROPS_URL)
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

    private void registerCodes(long dropId, String... codes) throws Exception {
        mockMvc.perform(post(ADMIN_DROPS_URL + "/" + dropId + "/codes")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(Map.of("codes", List.of(codes)))))
                .andExpect(status().isOk());
    }

    private void movePeriod(long dropId, String openAt, String closeAt) {
        jdbcTemplate.update("UPDATE drop_events SET open_at = ?, close_at = ? WHERE id = ?", openAt, closeAt, dropId);
    }

    private int assignedCount(long dropId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM prize_codes WHERE drop_event_id = ? AND assigned_user_id IS NOT NULL",
                Integer.class, dropId);
        return count == null ? 0 : count;
    }

    private String issueToken(String providerUserId, String role) throws Exception {
        String json = mockMvc.perform(post(TOKEN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"providerUserId":"%s","nickname":"%s","role":"%s"}
                                """.formatted(providerUserId, providerUserId, role)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return jsonMapper.readTree(json).get("accessToken").asString();
    }
}
