package io.github.hyinorbitcode.couponlab.global.security;

import io.github.hyinorbitcode.couponlab.global.error.CommonErrorCode;
import io.github.hyinorbitcode.couponlab.global.error.ErrorCode;
import io.github.hyinorbitcode.couponlab.global.error.Problems;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class SecurityProblemHandlers {

    private final JsonMapper jsonMapper;

    public AuthenticationEntryPoint authenticationEntryPoint() {
        BearerTokenAuthenticationEntryPoint bearer = new BearerTokenAuthenticationEntryPoint();
        return (request, response, exception) -> {
            bearer.commence(request, response, exception);
            write(request, response, CommonErrorCode.UNAUTHORIZED);
        };
    }

    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, exception) -> write(request, response, CommonErrorCode.FORBIDDEN);
    }

    private void write(HttpServletRequest request, HttpServletResponse response, ErrorCode errorCode)
            throws IOException {
        ProblemDetail problem = Problems.of(errorCode);
        problem.setInstance(URI.create(request.getRequestURI()));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("title", problem.getTitle());
        body.put("status", problem.getStatus());
        body.put("detail", problem.getDetail());
        body.put("instance", problem.getInstance().toString());
        body.putAll(problem.getProperties());

        response.setStatus(problem.getStatus());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        jsonMapper.writeValue(response.getWriter(), body);
    }
}