package io.github.hyinorbitcode.couponlab.global.error;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ErrorResponseTests.ErrorTestController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(ErrorResponseTests.ErrorTestController.class)
class ErrorResponseTests {

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("@RestController 경로 앞에 /api가 붙는다")
    void apiPrefix() throws Exception {
        mockMvc.perform(get("/api/error-test/ok")).andExpect(status().isOk());
        mockMvc.perform(get("/error-test/ok")).andExpect(status().isNotFound());
    }

    @RestController
    @RequestMapping("/error-test")
    static class ErrorTestController {

        @GetMapping("/ok")
        String ok() {
            return "ok";
        }

        @GetMapping("/business")
        void business() {
            throw new BusinessException(CommonErrorCode.CONFLICT, "이미 처리 중인 요청입니다.");
        }

        @PostMapping("/validation")
        void validation(@Valid @RequestBody NameRequest request) {
        }

        @GetMapping("/illegal-argument")
        void illegalArgument() {
            throw new IllegalArgumentException("nickname must not be blank");
        }

        @GetMapping("/unexpected")
        void unexpected() {
            throw new IllegalStateException("secret internal detail");
        }

    }

    record NameRequest(@NotBlank String name) {
    }

    @Test
    @DisplayName("업무 예외는 ErrorCode의 상태와 코드, 예외 메시지로 응답한다")
    void businessException() throws Exception {
        mockMvc.perform(get("/api/error-test/business")).andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value("CONFLICT"))
                .andExpect(jsonPath("$.detail").value("이미 처리 중인 요청입니다."));
    }

    @Test
    @DisplayName("IllegalArgumentException은 400 INVALID_INPUT이며 내부 메시지를 숨긴다")
    void illegalArgument() throws Exception {
        mockMvc.perform(get("/api/error-test/illegal-argument"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.detail").value(not(containsString("must not be blank"))));
    }

    @Test
    @DisplayName("예상하지 못한 예외는 500 INTERNAL_ERROR이며 내부 메시지를 숨긴다")
    void unexpected() throws Exception {
        mockMvc.perform(get("/api/error-test/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.detail").value(not(containsString("secret"))));
    }

    @Test
    @DisplayName("@Valid 검증 실패는 400 INVALID_INPUT과 필드별 오류를 준다")
    void validationFailure() throws Exception {
        mockMvc.perform(post("/api/error-test/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"name": " "}
                            """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.errors[0].field").value("name"))
                .andExpect(jsonPath("$.errors[0].message").exists());
    }

    @Test
    @DisplayName("잘못된 JSON은 400 INVALID_INPUT과 일반 메시지를 준다")
    void malformedJson() throws Exception {
        mockMvc.perform(post("/api/error-test/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.detail").value("입력값을 확인해 주세요."));
    }

    @Test
    @DisplayName("없는 경로는 404 NOT_FOUND를 준다")
    void notFound() throws Exception {
        mockMvc.perform(get("/api/no-such-path"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("지원하지 않는 메서드는 405 METHOD_NOT_ALLOWED를 준다")
    void methodNotAllowed() throws Exception {
        mockMvc.perform(delete("/api/error-test/ok"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
    }
}
