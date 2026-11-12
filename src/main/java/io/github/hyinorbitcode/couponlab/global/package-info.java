/**
 * 전역 모듈: 모든 도메인 모듈이 쓰는 설정(config), 오류 규칙과 전역 예외 처리(error), 보안 응답(security).
 * OPEN 모듈이라 다른 모듈이 하위 패키지까지 참조할 수 있다. 도메인 모듈을 참조하지 않는다.
 */
@ApplicationModule(type = ApplicationModule.Type.OPEN)
package io.github.hyinorbitcode.couponlab.global;

import org.springframework.modulith.ApplicationModule;
