# ADR-0008 패키지 구조: 모듈은 기능 단위, 모듈 안은 역할 단위

작성일: 2026-10-30
상태: 대체됨 (ADR-0014). 컨트롤러·서비스 분리 등 일부 원칙은 ADR-0014가 이어받음
보강: 2026-10-30 컨트롤러·서비스 분리 기준 추가
보강: 2026-10-30 웹 DTO를 web/dto 패키지로 분리, 역할 패키지 의존 방향을 ArchUnit으로 검사

## 맥락

첫 모듈(`user`)을 구현하면서 컨트롤러·설정·엔티티·서비스를 `user/internal` 한 패키지에 두었다. 클래스를 package-private으로 숨길 수 있다는 장점이 있지만, 회원 탈퇴·카카오 로그인처럼 기능이 늘면 역할이 섞여 찾기 어렵다. 이후 구현할 `drop`, `prizecode`, `participation` 모듈이 따를 규칙도 필요하다.

## 결정

### 1. 최상위는 기능(도메인) 단위 모듈

`user`, `drop`, `prizecode`, `participation`. 모듈 경계는 Spring Modulith `ApplicationModules.verify()`가 테스트에서 검사한다(ADR-0001).

### 2. 모듈 안의 구조

```
<module>/
├── 공개 API         다른 모듈이 쓰는 타입만 (예: user.CurrentUser, user.Role)
├── 공개 이벤트      모듈 밖에 알릴 사건 (예: 나중에 user.UserWithdrawn)
└── internal/
    ├── web/          컨트롤러, 요청·응답 DTO, 인자 리졸버, MVC 설정
    ├── application/  유스케이스 서비스, 트랜잭션 경계, 애플리케이션 예외
    ├── domain/       엔티티, 값 객체, 도메인 규칙, 리포지토리 인터페이스
    └── (모듈 고유 역할) 예: user/internal/security
```

| 패키지 | 책임 | 의존 방향 |
| --- | --- | --- |
| `web` | HTTP 입출력만. 비즈니스 판단을 하지 않는다 | → `application` (응답을 만들기 위해 `domain` 타입 읽기는 허용) |
| `application` | 유스케이스와 트랜잭션 경계 | → `domain` |
| `domain` | 규칙 자체 | 다른 역할 패키지를 참조하지 않는다 |

- 컨트롤러는 리포지토리를 직접 쓰지 않고 `application` 서비스를 거친다.
- **컨트롤러는 URL·리소스 단위, 서비스는 유스케이스 단위로 나눈다.** 컨트롤러는 HTTP 입출력만 하는 얇은 계층이라 URL 구조를 따라 묶고(예: `/api/auth/**` → `AuthController`, `/api/users/**` → `UserController`), 로직이 들어가는 서비스는 유스케이스별로 나눈다(예: `SignupService`, `EmailVerificationService`). URL 경계는 보안 규칙(인증 필요 여부)과도 맞춘다. 프로필처럼 클래스 단위로 붙는 조건이 다른 엔드포인트는 별도 컨트롤러로 둔다(예: `ExperimentAuthController`). 한 컨트롤러가 지나치게 커지면 성격이 다른 부분만 떼어 낸다(예: 나중에 카카오 OAuth 콜백).
- 한 클래스는 한 역할만 한다. 예: 인자 리졸버와 그것을 MVC에 등록하는 설정은 별도 클래스(`CurrentUserArgumentResolver`, `UserWebMvcConfig`).
- **웹 요청·응답 DTO는 `web/dto` 패키지에 클래스마다 별도 파일(record)로 둔다.** 이름은 `XxxRequest`, `XxxResponse`. 하위 패키지라 `public`이 되므로, `application`·`domain`이 `web`(DTO 포함)을 참조하지 못하게 `LayerDependencyTests`(ArchUnit)로 막는다. 서비스는 웹 DTO 대신 원시 값이나 `application`에 정의한 커맨드 객체를 받는다. 입력 정리(예: 이메일 앞뒤 공백 제거)처럼 검증 전에 필요한 처리는 DTO의 compact 생성자에 둔다.

### 3. 접근 제어

하위 패키지끼리 참조하는 타입은 `public`으로 둔다. 모듈 밖으로의 노출은 `internal` 아래 모든 패키지를 비공개로 취급하는 Spring Modulith 경계 테스트가 막는다. 같은 하위 패키지 안에서만 쓰는 타입(컨트롤러, 설정 클래스, DTO 등)은 package-private으로 둔다.

### 4. 모듈 간 통신

- 조회처럼 결과가 필요한 호출은 상대 모듈의 공개 API를 쓴다.
- 상태 변화를 알리는 경우(예: 회원 탈퇴 → 당첨 기록 익명화)는 직접 호출하지 않고 공개 이벤트를 발행하고 필요한 모듈이 구독한다.

### 5. 모듈이 커질 때

하위 패키지를 더 깊게 나누기보다 모듈을 분리한다. 예: `user` 안에서 인증(토큰, 카카오 로그인)과 계정 관리(프로필, 탈퇴)가 각각 커지면 `auth`와 `user` 모듈로 나눈다.

## 근거

- 기능 단위 모듈은 변경이 한 모듈 안에서 끝나게 하고, 모듈 안의 역할 단위 구조는 처음 보는 사람도 익숙한 계층으로 빠르게 읽게 한다.
- 모든 모듈이 같은 모양이면 새 모듈을 만들 때 고민할 것이 줄고 코드 리뷰가 쉬워진다.
- 클래스가 적을 때 나누는 비용이 가장 작다.

## 검토 후 기각한 대안

- **모듈 안을 평평한 한 패키지로 유지:** package-private으로 이중 보호가 되지만, 기능이 늘면 역할이 섞인다.
- **모듈 안을 기능 단위로 다시 나눔(예: `user/internal/withdrawal`):** 모듈 안의 기능이 그만큼 커지면 모듈 분리가 더 명확한 해법이다.
- **최상위를 계층 단위로 나눔(`controller/`, `service/`, `repository/`):** 기능 하나를 바꿀 때 여러 최상위 패키지를 오가야 하고, 모듈 경계를 검사할 수 없다.

## DTO 위치를 정할 때 검토한 대안

| 안 | 장단점 | 결정 |
| --- | --- | --- |
| 컨트롤러 안 중첩 record | 사용처 옆에 있어 읽기 쉽고 package-private 유지. 컨트롤러가 길어지고 공유가 어렵다 | 기각 |
| **`web/dto` 별도 파일** | DTO를 모아 보기 쉽고 국내 실무에서 익숙한 구조. `public`이 강제된다 | **채택**. 노출 문제는 ArchUnit 테스트로 보완 |
| `web` 패키지 별도 파일 | package-private 유지. `web` 패키지 파일이 늘어난다 | 기각 |

## 결과

- `user` 모듈을 이 구조로 옮겼다. 컨트롤러의 리포지토리 직접 사용을 `UserService.get()`으로 바꾸고 `UserNotFoundException`(404)을 추가했다.
- 테스트도 대상 클래스와 같은 하위 패키지에 둔다.
- 모듈 간 경계는 Spring Modulith(`ModularityTests`), 모듈 안의 역할 패키지 방향(application·domain → web 금지, domain → application 금지)은 ArchUnit(`LayerDependencyTests`)이 검사한다.
