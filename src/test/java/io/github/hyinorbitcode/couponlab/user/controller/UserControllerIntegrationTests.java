package io.github.hyinorbitcode.couponlab.user.controller;

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


import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class UserControllerIntegrationTests {

    private static final String ME_URL = "/api/users/me";
    private static final String TOKEN_URL = "/api/experiment/auth/token";

    @Autowired
    MockMvc mockMvc;
    @Autowired
    JsonMapper jsonMapper;
    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        DatabaseCleaner.truncateAll(jdbcTemplate);
    }

    private String issueToken(String providerUserId, String nickname, String role) throws Exception {
        String json = mockMvc.perform(post(TOKEN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"providerUserId":"%s","nickname":"%s","role":"%s"}
                                """.formatted(providerUserId, nickname, role)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return jsonMapper.readTree(json).get("accessToken").asString();
    }

    @Test
    @DisplayName("토큰으로 요청하면 내 정보를 전달한다")
    void returnsMe() throws Exception {
        String token = issueToken("u-1", "tester", "USER");

        mockMvc.perform(get(ME_URL).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.nickname").value("tester"))
                .andExpect(jsonPath("$.role").value("USER"));

    }

    @Test
    @DisplayName("토큰 없이 요청하면 401을 받는다")
    void rejectsWithoutToken() throws Exception {
        mockMvc.perform(get(ME_URL))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("토큰은 유효하지만 사용자가 없으면 404를 받는다")
    void returnsNotFoundWhenUserDeleted() throws Exception {
        String token = issueToken("u-1", "tester", "USER");
        DatabaseCleaner.truncateAll(jdbcTemplate);

        mockMvc.perform(get(ME_URL).header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
    }



}
