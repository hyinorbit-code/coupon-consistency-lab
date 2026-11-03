package io.github.hyinorbitcode.couponlab.user.controller;

import io.github.hyinorbitcode.couponlab.TestcontainersConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class ExperimentAuthDisabledTests {

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("experiment 프로필이 없으면 실험용 토큰 발급 API가 존재하지 않는다")
    void notAvailableWithoutExperimentProfile() throws Exception {
        mockMvc.perform(post("/api/experiment/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"providerUserId":"u-1","nickname":"tester","role":"ADMIN"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }
}
