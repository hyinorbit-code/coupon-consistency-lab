package io.github.hyinorbitcode.couponlab.prizecode.controller;

import io.github.hyinorbitcode.couponlab.DatabaseCleaner;
import io.github.hyinorbitcode.couponlab.IntegrationTest;
import io.github.hyinorbitcode.couponlab.prizecode.entity.PrizeCode;
import io.github.hyinorbitcode.couponlab.prizecode.repository.PrizeCodeRepository;
import io.github.hyinorbitcode.couponlab.prizecode.security.PrizeCodeCipher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class AdminPrizeCodeIntegrationTests {

    private static final String TOKEN_URL = "/api/experiment/auth/token";
    private static final String DROPS_URL = "/api/admin/drops";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JsonMapper jsonMapper;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    PrizeCodeRepository prizeCodeRepository;

    @Autowired
    PrizeCodeCipher cipher;

    private String admin;

    @BeforeEach
    void setUp() throws Exception {
        DatabaseCleaner.truncateAll(jdbcTemplate);
        admin = issueToken("ADMIN");
    }

    @Test
    @DisplayName("코드를 등록하면 200과 등록 개수를 주고 코드를 저장한다")
    void registersCodes() throws Exception {
        long dropId = createDrop();

        registerCodes(admin, dropId, codesJson("COFFEE-0001", "COFFEE-0002", "COFFEE-0003"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registered").value(3))
                .andExpect(jsonPath("$.duplicates").value(0));

        List<PrizeCode> saved = prizeCodeRepository.findAll();
        assertThat(saved).hasSize(3);
        assertThat(saved).allSatisfy(code -> {
            assertThat(code.getDropEventId()).isEqualTo(dropId);
            assertThat(code.isAssigned()).isFalse();
        });
    }

    @Test
    @DisplayName("원문은 암호화해 저장하고 복호화하면 원문, 힌트는 끝 4자리다")
    void storesEncryptedCodeWithHint() throws Exception {
        long dropId = createDrop();

        registerCodes(admin, dropId, codesJson("COFFEE-0001"))
                .andExpect(status().isOk());

        PrizeCode saved = prizeCodeRepository.findAll().getFirst();
        String storedAsText = new String(saved.getCodeCiphertext(), StandardCharsets.ISO_8859_1);
        assertThat(storedAsText).doesNotContain("COFFEE-0001");
        assertThat(cipher.decrypt(saved.getCodeCiphertext())).isEqualTo("COFFEE-0001");
        assertThat(saved.getCodeHint()).isEqualTo("0001");
        assertThat(saved.getCodeHash()).hasSize(PrizeCode.HASH_BYTES);
    }

    @Test
    @DisplayName("한 요청 안에 같은 코드가 있으면 하나만 등록하고 나머지는 중복으로 센다")
    void countsDuplicatesInRequest() throws Exception {
        long dropId = createDrop();

        registerCodes(admin, dropId, codesJson("COFFEE-0001", "COFFEE-0001", "COFFEE-0002"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registered").value(2))
                .andExpect(jsonPath("$.duplicates").value(1));

        assertThat(prizeCodeRepository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("앞뒤 공백만 다른 코드는 같은 코드로 보고 공백을 지워 저장한다")
    void stripsSurroundingWhitespace() throws Exception {
        long dropId = createDrop();

        registerCodes(admin, dropId, codesJson("COFFEE-0001", "  COFFEE-0001  "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registered").value(1))
                .andExpect(jsonPath("$.duplicates").value(1));

        PrizeCode saved = prizeCodeRepository.findAll().getFirst();
        assertThat(cipher.decrypt(saved.getCodeCiphertext())).isEqualTo("COFFEE-0001");
    }

    @Test
    @DisplayName("이미 등록한 코드를 다시 보내면 중복으로 세고 새로 저장하지 않는다")
    void countsDuplicatesAlreadyStored() throws Exception {
        long dropId = createDrop();
        String body = codesJson("COFFEE-0001", "COFFEE-0002", "COFFEE-0003");
        registerCodes(admin, dropId, body).andExpect(status().isOk());

        registerCodes(admin, dropId, body)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registered").value(0))
                .andExpect(jsonPath("$.duplicates").value(3));

        assertThat(prizeCodeRepository.count()).isEqualTo(3);
    }

    @Test
    @DisplayName("다른 이벤트에 등록한 코드도 중복으로 본다")
    void countsDuplicatesAcrossDrops() throws Exception {
        long firstDrop = createDrop();
        long secondDrop = createDrop();
        registerCodes(admin, firstDrop, codesJson("COFFEE-0001")).andExpect(status().isOk());

        registerCodes(admin, secondDrop, codesJson("COFFEE-0001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registered").value(0))
                .andExpect(jsonPath("$.duplicates").value(1));
    }

    @Test
    @DisplayName("없는 이벤트에 등록하면 404를 받는다")
    void rejectsUnknownDrop() throws Exception {
        registerCodes(admin, 999999L, codesJson("COFFEE-0001"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("DROP_NOT_FOUND"));

        assertThat(prizeCodeRepository.count()).isZero();
    }

    @Test
    @DisplayName("취소한 이벤트에 등록하면 409를 받고 아무것도 저장하지 않는다")
    void rejectsCanceledDrop() throws Exception {
        long dropId = createDrop();
        jdbcTemplate.update("UPDATE drop_events SET status = 'CANCELED' WHERE id = ?", dropId);

        registerCodes(admin, dropId, codesJson("COFFEE-0001"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CODES_LOCKED"));

        assertThat(prizeCodeRepository.count()).isZero();
    }

    @Test
    @DisplayName("오픈 시각이 지난 이벤트에 등록하면 409를 받는다")
    void rejectsOpenedDrop() throws Exception {
        long dropId = createDrop();
        jdbcTemplate.update(
                "UPDATE drop_events SET open_at = '2000-01-01 00:00:00', close_at = '2099-01-01 00:00:00' WHERE id = ?",
                dropId);

        registerCodes(admin, dropId, codesJson("COFFEE-0001"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CODES_LOCKED"));

        assertThat(prizeCodeRepository.count()).isZero();
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
            "{\"codes\":[]}",
            "{}",
            "{\"codes\":[\"COFFEE-0001\",\" \"]}"
    })
    @DisplayName("코드 목록이 비었거나 빈 코드가 있으면 400을 받는다")
    void rejectsInvalidRequest(String body) throws Exception {
        long dropId = createDrop();

        registerCodes(admin, dropId, body)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.errors[0].field", startsWith("codes")));

        assertThat(prizeCodeRepository.count()).isZero();
    }

    @Test
    @DisplayName("코드가 최대 길이를 넘으면 400을 받는다")
    void rejectsTooLongCode() throws Exception {
        long dropId = createDrop();

        registerCodes(admin, dropId, codesJson("A".repeat(PrizeCode.MAX_CODE_LENGTH + 1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));

        assertThat(prizeCodeRepository.count()).isZero();
    }

    @Test
    @DisplayName("일반 사용자가 등록하면 403을 받는다")
    void rejectsUser() throws Exception {
        long dropId = createDrop();
        String user = issueToken("USER");

        registerCodes(user, dropId, codesJson("COFFEE-0001"))
                .andExpect(status().isForbidden());

        assertThat(prizeCodeRepository.count()).isZero();
    }

    @Test
    @DisplayName("토큰 없이 등록하면 401을 받는다")
    void rejectsWithoutToken() throws Exception {
        long dropId = createDrop();

        mockMvc.perform(post(codesUrl(dropId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(codesJson("COFFEE-0001")))
                .andExpect(status().isUnauthorized());

        assertThat(prizeCodeRepository.count()).isZero();
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

    private ResultActions registerCodes(String token, long dropId, String body) throws Exception {
        return mockMvc.perform(post(codesUrl(dropId))
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
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

    private static String codesUrl(long dropId) {
        return DROPS_URL + "/" + dropId + "/codes";
    }

    private String codesJson(String... codes) {
        return jsonMapper.writeValueAsString(Map.of("codes", List.of(codes)));
    }
}
