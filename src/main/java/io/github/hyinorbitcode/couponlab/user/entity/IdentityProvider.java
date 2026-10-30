package io.github.hyinorbitcode.couponlab.user.entity;

/**
 * 사용자에 연동되는 외부 계정의 제공자. 응모 자격은 KAKAO 연동으로 판단한다(ADR-0010).
 */
public enum IdentityProvider {
    KAKAO,
    EXPERIMENT
}
