package io.github.hyinorbitcode.couponlab.drop.controller;

import io.github.hyinorbitcode.couponlab.DatabaseCleaner;
import io.github.hyinorbitcode.couponlab.IntegrationTest;
import io.github.hyinorbitcode.couponlab.drop.DropMetrics;
import io.micrometer.core.instrument.MeterRegistry;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 관리자 목록, 상세(코드 수, 당첨자, 응모 시도 수), 기본 대사. */
@IntegrationTest
class AdminDropOverviewIntegrationTests {

    private static final String TOKEN_URL = "/api/experiment/auth/token";
    private static final String ADMIN_DROPS_URL = "/api/admin/drops";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JsonMapper jsonMapper;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    MeterRegistry meterRegistry;

    private String admin;

    @BeforeEach
    void setUp() throws Exception {
        DatabaseCleaner.truncateAll(jdbcTemplate);
        // DB를 비우면 이벤트 ID가 1부터 다시 시작하므로, 이전 테스트의 응모 시도 수 메트릭도 지운다.
        meterRegistry.find(DropMetrics.ENTRY_ATTEMPTS).meters().forEach(meterRegistry::remove);
        admin = issueToken("admin-1", "ADMIN");
    }

    @Test
    @DisplayName("관리자 목록은 모든 상태의 이벤트를 오픈 시각 내림차순으로 코드 수와 함께 준다")
    void listsAllDropsForAdmin() throws Exception {
        long draft = createDrop("초안", "2099-03-01T00:00:00Z", "2099-03-01T01:00:00Z");
        long open = openDrop("진행 중", "C-1", "C-2");
        enter(issueToken("user-1", "USER"), open);
        long canceled = createDrop("취소", "2099-02-01T00:00:00Z", "2099-02-01T01:00:00Z");
        registerCodes(canceled, "X-1");
        adminAction(canceled, "cancel");

        mockMvc.perform(get(ADMIN_DROPS_URL).header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].id").value(draft))
                .andExpect(jsonPath("$[0].status").value("DRAFT"))
                .andExpect(jsonPath("$[0].phase").value("UPCOMING"))
                .andExpect(jsonPath("$[0].registeredCodes").value(0))
                .andExpect(jsonPath("$[1].id").value(canceled))
                .andExpect(jsonPath("$[1].status").value("CANCELED"))
                .andExpect(jsonPath("$[1].registeredCodes").value(1))
                .andExpect(jsonPath("$[2].id").value(open))
                .andExpect(jsonPath("$[2].status").value("PUBLISHED"))
                .andExpect(jsonPath("$[2].phase").value("OPEN"))
                .andExpect(jsonPath("$[2].registeredCodes").value(2))
                .andExpect(jsonPath("$[2].assignedCodes").value(1));
    }

    @Test
    @DisplayName("관리자 상세는 코드 수, 당첨자 닉네임과 코드 끝 4자리, 할당 순서를 준다")
    void detailShowsWinners() throws Exception {
        long dropId = openDrop("커피 드롭", "COFFEE-0001", "COFFEE-0002", "COFFEE-0003");
        enter(issueToken("user-1", "USER", "첫번째"), dropId);
        enter(issueToken("user-2", "USER", "두번째"), dropId);

        mockMvc.perform(get(ADMIN_DROPS_URL + "/" + dropId).header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registeredCodes").value(3))
                .andExpect(jsonPath("$.assignedCodes").value(2))
                .andExpect(jsonPath("$.phase").value("OPEN"))
                .andExpect(jsonPath("$.prizeDescription").value("아메리카노 쿠폰"))
                .andExpect(jsonPath("$.winners", hasSize(2)))
                .andExpect(jsonPath("$.winners[0].nickname").value("첫번째"))
                .andExpect(jsonPath("$.winners[0].codeHint").value("0001"))
                .andExpect(jsonPath("$.winners[0].assignedAt").isNotEmpty())
                .andExpect(jsonPath("$.winners[1].nickname").value("두번째"))
                .andExpect(jsonPath("$.winners[1].codeHint").value("0002"));
    }

    @Test
    @DisplayName("응모 시도 수는 매진 응답과 재응모까지 포함해 센다")
    void countsEntryAttemptsIncludingSoldOut() throws Exception {
        long dropId = openDrop("커피 드롭", "C-1");
        String winner = issueToken("user-1", "USER");
        enter(winner, dropId);
        mockMvc.perform(post("/api/drops/" + dropId + "/entries").header("Authorization", "Bearer " + winner))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/drops/" + dropId + "/entries")
                        .header("Authorization", "Bearer " + issueToken("user-2", "USER")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SOLD_OUT"));

        mockMvc.perform(get(ADMIN_DROPS_URL + "/" + dropId).header("Authorization", "Bearer " + admin))
                .andExpect(jsonPath("$.entryAttempts").value(3))
                .andExpect(jsonPath("$.assignedCodes").value(1))
                .andExpect(jsonPath("$.phase").value("SOLD_OUT"));
    }

    @Test
    @DisplayName("생성·공개·취소 응답도 같은 상세 형식으로 코드 수를 준다")
    void statusChangeResponsesIncludeCounts() throws Exception {
        long dropId = createDrop("커피 드롭", "2099-01-01T00:00:00Z", "2099-01-01T01:00:00Z");
        registerCodes(dropId, "C-1", "C-2");

        mockMvc.perform(post(ADMIN_DROPS_URL + "/" + dropId + "/publish").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.registeredCodes").value(2))
                .andExpect(jsonPath("$.assignedCodes").value(0))
                .andExpect(jsonPath("$.entryAttempts").value(0))
                .andExpect(jsonPath("$.winners", hasSize(0)));
    }

    @Test
    @DisplayName("정상 응모 뒤 기본 대사는 모든 항목이 맞는다")
    void reconciliationConsistent() throws Exception {
        long dropId = openDrop("커피 드롭", "C-1", "C-2", "C-3");
        enter(issueToken("user-1", "USER"), dropId);
        enter(issueToken("user-2", "USER"), dropId);

        mockMvc.perform(get(ADMIN_DROPS_URL + "/" + dropId + "/reconciliation").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dropId").value(dropId))
                .andExpect(jsonPath("$.consistent").value(true))
                .andExpect(jsonPath("$.registeredCodes").value(3))
                .andExpect(jsonPath("$.assignedCodes").value(2))
                .andExpect(jsonPath("$.distinctWinners").value(2))
                .andExpect(jsonPath("$.incompleteAssignments").value(0))
                .andExpect(jsonPath("$.assignedOutsidePeriod").value(0))
                .andExpect(jsonPath("$.assignedWithinRegistered").value(true))
                .andExpect(jsonPath("$.oneWinPerUser").value(true))
                .andExpect(jsonPath("$.assignmentsComplete").value(true))
                .andExpect(jsonPath("$.assignedWithinPeriod").value(true));
    }

    @Test
    @DisplayName("기간 밖에 할당된 코드가 있으면 기본 대사가 어긋남을 알려 준다")
    void reconciliationDetectsAssignmentOutsidePeriod() throws Exception {
        long dropId = openDrop("커피 드롭", "C-1", "C-2");
        enter(issueToken("user-1", "USER"), dropId);
        jdbcTemplate.update("UPDATE prize_codes SET assigned_at = '1999-12-31 00:00:00' WHERE drop_event_id = ? AND assigned_user_id IS NOT NULL", dropId);

        mockMvc.perform(get(ADMIN_DROPS_URL + "/" + dropId + "/reconciliation").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.consistent").value(false))
                .andExpect(jsonPath("$.assignedOutsidePeriod").value(1))
                .andExpect(jsonPath("$.assignedWithinPeriod").value(false))
                .andExpect(jsonPath("$.oneWinPerUser").value(true));
    }

    @Test
    @DisplayName("없는 이벤트의 상세와 대사는 404, 일반 사용자는 403이다")
    void rejectsUnknownAndNonAdmin() throws Exception {
        mockMvc.perform(get(ADMIN_DROPS_URL + "/999999/reconciliation").header("Authorization", "Bearer " + admin))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("DROP_NOT_FOUND"));

        String user = issueToken("user-1", "USER");
        mockMvc.perform(get(ADMIN_DROPS_URL).header("Authorization", "Bearer " + user))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(ADMIN_DROPS_URL + "/1/reconciliation").header("Authorization", "Bearer " + user))
                .andExpect(status().isForbidden());
    }

    private long openDrop(String title, String... codes) throws Exception {
        long dropId = createDrop(title, "2099-01-01T00:00:00Z", "2099-01-01T01:00:00Z");
        registerCodes(dropId, codes);
        adminAction(dropId, "publish");
        jdbcTemplate.update("UPDATE drop_events SET open_at = '2000-01-01 00:00:00', close_at = '2099-01-01 00:00:00' WHERE id = ?", dropId);
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

    private void registerCodes(long dropId, String... codes) throws Exception {
        mockMvc.perform(post(ADMIN_DROPS_URL + "/" + dropId + "/codes")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(Map.of("codes", List.of(codes)))))
                .andExpect(status().isOk());
    }

    private void adminAction(long dropId, String action) throws Exception {
        mockMvc.perform(post(ADMIN_DROPS_URL + "/" + dropId + "/" + action).header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk());
    }

    private void enter(String token, long dropId) throws Exception {
        mockMvc.perform(post("/api/drops/" + dropId + "/entries").header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated());
    }

    private String issueToken(String providerUserId, String role) throws Exception {
        return issueToken(providerUserId, role, providerUserId);
    }

    private String issueToken(String providerUserId, String role, String nickname) throws Exception {
        String json = mockMvc.perform(post(TOKEN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(Map.of(
                                "providerUserId", providerUserId, "nickname", nickname, "role", role))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return jsonMapper.readTree(json).get("accessToken").asString();
    }
}
