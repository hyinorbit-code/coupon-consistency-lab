# ADR-0007 인증: 무상태 JWT와 실험용 토큰 발급

작성일: 2026-10-30
상태: 채택

## 맥락

모든 API는 호출자와 역할(ADMIN, USER)을 알아야 한다. 실서비스 로그인은 카카오 로그인이지만 E단계에서 연동하기로 했고(ADR-0003), 1~3단계는 실험용 계정으로 진행한다. 부하 실험에서는 수만 명의 사용자 토큰을 바로 만들 수 있어야 한다.

## 결정

1. **무상태 JWT 인증.** Spring Security OAuth2 Resource Server로 Bearer 토큰을 검증한다. 세션과 쿠키를 쓰지 않으므로 CSRF 보호는 끈다.
2. **발급과 검증 모두 Nimbus(Spring Security 내장)를 쓴다.** 별도 JWT 라이브러리(jjwt)를 추가하지 않는다. 발급자를 바꿀 때도 디코더 설정만 바꾸면 된다.
3. **서명은 HS256 대칭키.** 발급자와 검증자가 같은 애플리케이션이라 비대칭키가 필요 없다. 키는 `COUPONLAB_AUTH_JWT_SECRET` 환경변수로 주입하고, 32바이트보다 짧으면 설정 바인딩 단계에서 실패해 기동하지 않는다.
4. **클레임:** `sub`(사용자 ID), `role`(ADMIN·USER), `iss`, `iat`, `exp`. 검증은 서명, 만료(주입된 `Clock` 기준), 발급자를 확인한다. `role`은 `ROLE_ADMIN`·`ROLE_USER` 권한으로 변환한다.
5. **유효기간 24시간**(`couponlab.auth.token-ttl`). 실험 편의를 위한 값이며 카카오 로그인 연동 시 다시 정한다.
6. **실험용 토큰 발급은 `experiment` 프로필에서만 존재한다.** `POST /experiment/auth/token`은 카카오 첫 로그인처럼 사용자가 없으면 만들고, 있으면 기존 사용자에게 토큰만 발급한다. 기존 사용자의 닉네임·역할은 바꾸지 않는다.
7. **권한 규칙:** `/experiment/**`, `/actuator/health`, `/error`는 허용, `/admin/**`는 ADMIN, 나머지는 인증 필요.
8. **모듈 경계:** 다른 모듈은 `user.CurrentUser`(사용자 ID, 역할)만 참조한다. 컨트롤러 메서드에 `CurrentUser` 파라미터를 선언하면 JWT에서 채워진다. 사용자 엔티티·리포지토리는 `user.internal`에 두어 Spring Modulith 경계 테스트가 외부 참조를 막는다.

## 동시 첫 로그인

같은 식별자의 첫 로그인이 동시에 들어오면 둘 다 "사용자 없음"을 보고 INSERT할 수 있다. `UNIQUE(auth_provider, provider_user_id)`가 한쪽을 막고, 막힌 쪽은 먼저 커밋된 사용자를 다시 조회해 반환한다. 실패한 INSERT의 트랜잭션은 롤백되므로 재조회가 새 트랜잭션에서 실행되도록 `findOrCreate` 전체를 하나의 트랜잭션으로 묶지 않는다.

`UserConcurrencyTests`에서 20개 스레드가 동시에 같은 식별자로 첫 로그인했을 때, 19건이 유니크 제약에 막히고 모두 같은 사용자를 받으며 사용자는 1명만 생기는 것을 확인했다. 선착순 할당보다 작은 규모로 "DB 제약을 최후 방어선으로 두고 충돌을 복구하는" 패턴을 먼저 적용한 사례다.

## 검토 후 기각한 대안

- **세션 기반 인증:** 다중 API 인스턴스에서 세션 공유 저장소가 필요해 실험 구성이 복잡해진다.
- **jjwt 등 별도 JWT 라이브러리:** Spring Security가 발급·검증을 모두 제공하므로 의존성을 늘릴 이유가 없다.
- **RS256 비대칭키:** 토큰을 외부 서비스가 검증하는 구조가 아니다. 7단계에서 서비스를 분리하면 재검토한다.
- **요청 파라미터로 userId 전달 후 나중에 인증 추가:** 이후 모든 컨트롤러와 테스트를 고쳐야 한다.

## 결과

- 통합 테스트는 `@IntegrationTest`(Testcontainers MySQL + `test`·`experiment` 프로필)를 공통으로 쓴다. 테스트 전용 시크릿은 `src/test/resources/application-test.yml`에 있다.
- `/admin/**` 권한 거부(403) 테스트는 관리자 API를 구현할 때 추가한다.
- E단계에서 카카오 로그인 콜백이 같은 `findOrCreate`(제공자 `KAKAO`)와 `JwtTokenIssuer`를 쓰도록 연결한다.
