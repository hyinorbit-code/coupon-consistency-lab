package io.github.hyinorbitcode.couponlab.global.security;

import io.github.hyinorbitcode.couponlab.global.error.CommonErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Spring Security 필터에서 나가는 401·403 응답도 컨트롤러 오류와 같은 ProblemDetail + code 형식으로 만든다(ADR-0013).
 * 필터는 컨트롤러 예외 핸들러를 거치지 않으므로 보안 설정에 이 핸들러를 등록한다.
 */
@Component
@RequiredArgsConstructor
public class SecurityProblemHandlers {

    private final JsonMapper jsonMapper;

    /** 401. 표준 Bearer 진입점이 WWW-Authenticate 헤더를 채운 뒤 본문만 ProblemDetail로 쓴다. */
    public AuthenticationEntryPoint authenticationEntryPoint() {
        BearerTokenAuthenticationEntryPoint bearer = new BearerTokenAuthenticationEntryPoint();
        return (request, response, exception) -> {
            bearer.commence(request, response, exception);
            write(request, response, CommonErrorCode.UNAUTHORIZED, exception);
        };
    }

    /** 403. */
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, exception) -> write(request, response, CommonErrorCode.FORBIDDEN, exception);
    }

    private void write(HttpServletRequest request, HttpServletResponse response, CommonErrorCode code,
            RuntimeException cause) throws IOException {
        if (!(cause instanceof AuthenticationException || cause instanceof AccessDeniedException)) {
            throw cause;
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", "about:blank");
        body.put("title", code.status().getReasonPhrase());
        body.put("status", code.status().value());
        body.put("detail", code.defaultMessage());
        body.put("instance", request.getRequestURI());
        body.put("code", code.code());

        response.setStatus(code.status().value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(jsonMapper.writeValueAsString(body));
    }
}
