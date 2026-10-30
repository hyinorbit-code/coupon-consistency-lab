package io.github.hyinorbitcode.couponlab.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerTypePredicate;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 모든 {@link RestController} 경로 앞에 /api를 붙인다. 운영에서는 CloudFront가 /api/*만 백엔드로 보내고,
 * 로컬에서는 Vite 개발 서버가 같은 경로를 프록시한다(ADR-0011). Actuator는 대상이 아니다.
 */
@Configuration(proxyBeanMethods = false)
public class ApiPathConfiguration implements WebMvcConfigurer {

    public static final String API_PREFIX = "/api";

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.addPathPrefix(API_PREFIX, HandlerTypePredicate.forAnnotation(RestController.class));
    }
}
