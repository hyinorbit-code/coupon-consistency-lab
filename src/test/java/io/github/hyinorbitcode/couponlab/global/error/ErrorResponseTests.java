package io.github.hyinorbitcode.couponlab.global.error;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import io.github.hyinorbitcode.couponlab.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * ADR-0013: 모든 오류가 ProblemDetail + code 형식으로 나가는지 검증한다.
 */
@IntegrationTest
class ErrorResponseTests {

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("@Valid 검증 실패는 400 INVALID_INPUT과 필드별 오류를 준다")
    void validationFailure() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "not-an-email", "password": "correct-horse-battery", "nickname": "닉네임"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.errors[0].field").value("email"))
                .andExpect(jsonPath("$.errors[0].message").exists());
    }

    @Test
    @DisplayName("잘못된 JSON은 400 INVALID_INPUT을 준다")
    void malformedJson() throws Exception {
        mockMvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    @Test
    @DisplayName("업무 예외는 모듈이 정한 상태와 코드로 응답한다")
    void businessException() throws Exception {
        mockMvc.perform(post("/api/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token": "unknown"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_VERIFICATION_TOKEN"))
                .andExpect(jsonPath("$.detail").value(containsString("인증 링크")));
    }

    @Test
    @DisplayName("토큰 없이 보호된 API를 부르면 401 UNAUTHORIZED와 WWW-Authenticate 헤더를 준다")
    void unauthorized() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().exists(HttpHeaders.WWW_AUTHENTICATE))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("변조된 토큰도 401 UNAUTHORIZED 형식으로 응답한다")
    void invalidToken() throws Exception {
        mockMvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer not.a.jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("관리자 API를 일반 사용자가 부르면 403 FORBIDDEN을 준다")
    void forbidden() throws Exception {
        mockMvc.perform(get("/api/admin/__error-test").header(HttpHeaders.AUTHORIZATION, bearer(userToken())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("없는 경로는 404 NOT_FOUND를 준다")
    void notFound() throws Exception {
        mockMvc.perform(get("/api/no-such-path").header(HttpHeaders.AUTHORIZATION, bearer(userToken())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("도메인 값 검증 실패(IllegalArgumentException)는 400 INVALID_INPUT이고 내부 메시지를 노출하지 않는다")
    void illegalArgument() throws Exception {
        mockMvc.perform(get("/api/__error-test/illegal").header(HttpHeaders.AUTHORIZATION, bearer(userToken())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(content().string(not(containsString("internal-field"))));
    }

    @Test
    @DisplayName("예상하지 못한 예외는 500 INTERNAL_ERROR이고 예외 메시지와 스택을 노출하지 않는다")
    void unexpectedException() throws Exception {
        mockMvc.perform(get("/api/__error-test/boom").header(HttpHeaders.AUTHORIZATION, bearer(userToken())))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(content().string(not(containsString("db-password"))))
                .andExpect(content().string(not(containsString("at io.github"))));
    }

    @Test
    @DisplayName("BusinessException에 직접 준 메시지는 detail로 나간다")
    void businessExceptionWithCustomMessage() throws Exception {
        mockMvc.perform(get("/api/__error-test/conflict").header(HttpHeaders.AUTHORIZATION, bearer(userToken())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"))
                .andExpect(jsonPath("$.detail").value("이미 처리된 요청입니다."));
    }

    private String userToken() throws Exception {
        String body = mockMvc.perform(post("/api/experiment/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"providerUserId": "error-test-user", "nickname": "오류테스트", "role": "USER"}
                                """))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.accessToken");
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    /** 이 테스트에서만 등록되는 오류 발생용 엔드포인트. */
    @TestConfiguration(proxyBeanMethods = false)
    static class ErrorEndpointsConfiguration {

        @Bean
        ErrorEndpoints errorEndpoints() {
            return new ErrorEndpoints();
        }
    }

    @RestController
    static class ErrorEndpoints {

        @GetMapping("/__error-test/illegal")
        String illegal() {
            throw new IllegalArgumentException("internal-field must not be blank");
        }

        @GetMapping("/__error-test/boom")
        String boom() {
            throw new IllegalStateException("db-password=secret leaked");
        }

        @GetMapping("/__error-test/conflict")
        String conflict() {
            throw new BusinessException(CommonErrorCode.CONFLICT, "이미 처리된 요청입니다.");
        }

        @GetMapping("/admin/__error-test")
        String adminOnly() {
            return "ok";
        }
    }
}
