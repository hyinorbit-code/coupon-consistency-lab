package io.github.hyinorbitcode.couponlab.global.config;

import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param baseUrl 메일 링크가 가리키는 프론트엔드 주소(ADR-0011)
 */
@ConfigurationProperties("couponlab.web")
public record WebProperties(String baseUrl) {

    public WebProperties {
        Objects.requireNonNull(baseUrl, "couponlab.web.base-url");
        baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
}
