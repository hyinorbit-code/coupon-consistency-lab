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
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class DropQueryIntegrationTests {

    private static final String TOKEN_URL = "/api/experiment/auth/token";
    private static final String ADMIN_DROPS_URL = "/api/admin/drops";
    private static final String DROPS_URL = "/api/drops";

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
    @DisplayName("목록은 로그인 없이 볼 수 있고 공개된 이벤트만 오픈 시각 순으로 수량과 함께 준다")
    void listsPublishedDropsOnly() throws Exception {
        long later = publishedDrop("나중 드롭", "2099-02-01T00:00:00Z", "2099-02-01T01:00:00Z", "L-1", "L-2", "L-3");
        long earlier = publishedDrop("먼저 드롭", "2099-01-01T00:00:00Z", "2099-01-01T01:00:00Z", "E-1");
        createDrop("초안 드롭", "2099-01-15T00:00:00Z", "2099-01-15T01:00:00Z");
        long canceled = publishedDrop("취소 드롭", "2099-01-20T00:00:00Z", "2099-01-20T01:00:00Z", "C-1");
        adminAction(canceled, "cancel");

        mockMvc.perform(get(DROPS_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(earlier))
                .andExpect(jsonPath("$[0].title").value("먼저 드롭"))
                .andExpect(jsonPath("$[0].prizeDescription").value("아메리카노 쿠폰"))
                .andExpect(jsonPath("$[0].openAt").value("2099-01-01T00:00:00Z"))
                .andExpect(jsonPath("$[0].closeAt").value("2099-01-01T01:00:00Z"))
                .andExpect(jsonPath("$[0].totalQuantity").value(1))
                .andExpect(jsonPath("$[0].remainingQuantity").value(1))
                .andExpect(jsonPath("$[0].phase").value("UPCOMING"))
                .andExpect(jsonPath("$[1].id").value(later))
                .andExpect(jsonPath("$[1].totalQuantity").value(3))
                .andExpect(jsonPath("$[1].remainingQuantity").value(3));
    }

    @Test
    @DisplayName("공개된 이벤트가 없으면 빈 목록을 준다")
    void listsEmpty() throws Exception {
        createDrop("초안 드롭", "2099-01-15T00:00:00Z", "2099-01-15T01:00:00Z");

        mockMvc.perform(get(DROPS_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("응모가 진행되면 남은 수량이 줄고, 다 나가면 단계가 매진이 된다")
    void reflectsEntriesInQuantityAndPhase() throws Exception {
        long dropId = openDrop("C-1", "C-2");

        mockMvc.perform(get(DROPS_URL + "/" + dropId + "/stock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.remainingQuantity").value(2))
                .andExpect(jsonPath("$.phase").value("OPEN"));

        enter(issueToken("user-1", "USER"), dropId);
        mockMvc.perform(get(DROPS_URL + "/" + dropId + "/stock"))
                .andExpect(jsonPath("$.remainingQuantity").value(1))
                .andExpect(jsonPath("$.phase").value("OPEN"));

        enter(issueToken("user-2", "USER"), dropId);
        mockMvc.perform(get(DROPS_URL + "/" + dropId + "/stock"))
                .andExpect(jsonPath("$.remainingQuantity").value(0))
                .andExpect(jsonPath("$.phase").value("SOLD_OUT"));
        mockMvc.perform(get(DROPS_URL))
                .andExpect(jsonPath("$[0].totalQuantity").value(2))
                .andExpect(jsonPath("$[0].remainingQuantity").value(0))
                .andExpect(jsonPath("$[0].phase").value("SOLD_OUT"));
    }

    @Test
    @DisplayName("종료된 이벤트는 코드가 남아 있어도 종료 단계다")
    void closedDropStaysClosed() throws Exception {
        long dropId = publishedDrop("커피 드롭", "2099-01-01T00:00:00Z", "2099-01-01T01:00:00Z", "C-1");
        movePeriod(dropId, "2000-01-01 00:00:00", "2000-01-01 01:00:00");

        mockMvc.perform(get(DROPS_URL + "/" + dropId + "/stock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.remainingQuantity").value(1))
                .andExpect(jsonPath("$.phase").value("CLOSED"));
    }

    @Test
    @DisplayName("상세를 로그인 없이 보면 내 경품 ID는 null이다")
    void detailWithoutLogin() throws Exception {
        long dropId = openDrop("C-1", "C-2");

        mockMvc.perform(get(DROPS_URL + "/" + dropId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(dropId))
                .andExpect(jsonPath("$.title").value("커피 드롭"))
                .andExpect(jsonPath("$.totalQuantity").value(2))
                .andExpect(jsonPath("$.remainingQuantity").value(2))
                .andExpect(jsonPath("$.phase").value("OPEN"))
                .andExpect(jsonPath("$.myPrizeId").value(nullValue()));
    }

    @Test
    @DisplayName("당첨된 사용자가 로그인해서 상세를 보면 내 경품 ID를 주고, 다른 사용자에게는 null이다")
    void detailShowsMyPrize() throws Exception {
        long dropId = openDrop("C-1", "C-2");
        String winner = issueToken("user-1", "USER");
        long prizeId = jsonMapper.readTree(enter(winner, dropId)).get("prize").get("id").asLong();
        String other = issueToken("user-2", "USER");

        mockMvc.perform(get(DROPS_URL + "/" + dropId).header("Authorization", "Bearer " + winner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.myPrizeId").value(prizeId))
                .andExpect(jsonPath("$.remainingQuantity").value(1));
        mockMvc.perform(get(DROPS_URL + "/" + dropId).header("Authorization", "Bearer " + other))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.myPrizeId").value(nullValue()));
    }

    @Test
    @DisplayName("초안, 취소, 없는 이벤트는 상세와 수량 모두 404다")
    void hidesUnpublishedDrops() throws Exception {
        long draft = createDrop("초안 드롭", "2099-01-15T00:00:00Z", "2099-01-15T01:00:00Z");
        long canceled = publishedDrop("취소 드롭", "2099-01-20T00:00:00Z", "2099-01-20T01:00:00Z", "C-1");
        adminAction(canceled, "cancel");

        for (long id : List.of(draft, canceled, 999999L)) {
            mockMvc.perform(get(DROPS_URL + "/" + id))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("DROP_NOT_FOUND"));
            mockMvc.perform(get(DROPS_URL + "/" + id + "/stock"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("DROP_NOT_FOUND"));
        }
    }

    @Test
    @DisplayName("잘못된 토큰을 보내면 공개 API라도 401을 받는다")
    void rejectsInvalidTokenEvenOnPublicApi() throws Exception {
        mockMvc.perform(get(DROPS_URL).header("Authorization", "Bearer not-a-valid-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("응모는 여전히 로그인이 필요하다")
    void entryStillRequiresLogin() throws Exception {
        long dropId = openDrop("C-1");

        mockMvc.perform(post(DROPS_URL + "/" + dropId + "/entries"))
                .andExpect(status().isUnauthorized());
    }

    private long openDrop(String... codes) throws Exception {
        long dropId = publishedDrop("커피 드롭", "2099-01-01T00:00:00Z", "2099-01-01T01:00:00Z", codes);
        movePeriod(dropId, "2000-01-01 00:00:00", "2099-01-01 00:00:00");
        return dropId;
    }

    private long publishedDrop(String title, String openAt, String closeAt, String... codes) throws Exception {
        long dropId = createDrop(title, openAt, closeAt);
        mockMvc.perform(post(ADMIN_DROPS_URL + "/" + dropId + "/codes")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(Map.of("codes", List.of(codes)))))
                .andExpect(status().isOk());
        adminAction(dropId, "publish");
        return dropId;
    }

    private long createDrop(String title, String openAt, String closeAt) throws Exception {
        String json = mockMvc.perform(post(ADMIN_DROPS_URL)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(Map.of(
                                "title", title, "prizeDescription", "아메리카노 쿠폰",
                                "openAt", openAt, "closeAt", closeAt))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return jsonMapper.readTree(json).get("id").asLong();
    }

    private void adminAction(long dropId, String action) throws Exception {
        mockMvc.perform(post(ADMIN_DROPS_URL + "/" + dropId + "/" + action).header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk());
    }

    private String enter(String token, long dropId) throws Exception {
        return mockMvc.perform(post(DROPS_URL + "/" + dropId + "/entries").header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }

    private void movePeriod(long dropId, String openAt, String closeAt) {
        jdbcTemplate.update("UPDATE drop_events SET open_at = ?, close_at = ? WHERE id = ?", openAt, closeAt, dropId);
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
