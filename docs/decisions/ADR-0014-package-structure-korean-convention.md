# ADR-0014 패키지 구조: 국내 실무형 계층 패키지 + global

작성일: 2026-10-30
상태: 채택
대체: ADR-0008(패키지 구조) 전체, ADR-0013의 `common` 모듈 이름과 위치

## 맥락

ADR-0008은 모듈 안을 `internal/{web, application, domain, ...}`으로 나눴다. 오류 코드·설정·이벤트·메일 인터페이스가 `application`에 섞이기 시작했고, 국내 실무 코드와 모양이 달라 처음 보는 사람이 읽기 어렵다. 국내 실무에서 흔한 계층 패키지 이름과 `global` 공통 패키지 관례를 따르되, Spring Modulith 모듈 경계는 유지한다.

## 결정

### 1. 최상위

```
io.github.hyinorbitcode.couponlab
├── CouponLabApplication
├── global/          모든 모듈이 쓰는 공통 (OPEN 모듈)
│   ├── config/      ClockConfiguration, ApiPathConfiguration, WebProperties
│   ├── error/       ErrorCode, CommonErrorCode, BusinessException, GlobalExceptionHandler, Problems
│   └── security/    SecurityProblemHandlers
├── user/            도메인 모듈
├── drop/
├── prizecode/
└── participation/
```

- 도메인 모듈은 최상위 패키지다. Spring Modulith는 모듈의 하위 패키지를 모두 내부로 취급하므로 별도의 `internal` 패키지 없이도 경계가 유지된다. 다른 모듈은 모듈 바로 아래의 공개 타입(예: `user.CurrentUser`, `user.Role`)만 참조한다.
- `global`은 `@ApplicationModule(type = OPEN)`으로 지정해 다른 모듈이 하위 패키지까지 참조할 수 있게 한다. `global`은 도메인 모듈을 참조하지 않는다.

### 2. 도메인 모듈 안

```
user/
├── CurrentUser, Role          공개 API
├── controller/                컨트롤러
├── dto/
│   ├── request/               XxxRequest
│   └── response/              XxxResponse
├── service/                   유스케이스 서비스, 토큰 유틸
├── repository/                Spring Data 리포지토리
├── entity/                    JPA 엔티티, enum, 값 객체와 JPA 컨버터(예: EmailAddress)
├── exception/                 모듈 ErrorCode enum과 예외
├── config/                    모듈 설정값, MVC 설정, 인자 리졸버
├── event/                     모듈 내부 이벤트와 리스너
├── mail/                      메일 발송 인터페이스와 SMTP 구현
└── security/                  보안 설정, JWT 발급, 인증 설정값
```

모든 모듈이 이 이름을 쓰고, 필요 없는 패키지는 만들지 않는다.

### 3. 의존 방향 (`LayerDependencyTests`, ArchUnit)

| 규칙 | 이유 |
| --- | --- |
| controller·dto 밖에서는 controller·dto를 참조하지 않는다 | 웹 계층 타입이 서비스로 새지 않게. 서비스는 원시 값이나 서비스 쪽 커맨드 객체를 받는다 |
| entity·repository는 service를 참조하지 않는다 | 영속 계층이 유스케이스에 의존하지 않게 |
| entity는 repository를 참조하지 않는다 | 엔티티는 저장 방식을 모른다 |
| global은 도메인 모듈을 참조하지 않는다 | 공통 모듈의 순환 의존 방지 |

모듈 간 경계는 Spring Modulith(`ModularityTests`)가 검사한다.

### 4. 유지하는 규칙 (ADR-0008에서 이어짐)

- 컨트롤러는 URL·리소스 단위, 서비스는 유스케이스 단위로 나눈다. URL 경계는 보안 규칙과 맞춘다.
- 컨트롤러는 리포지토리를 직접 쓰지 않고 서비스를 거친다.
- 한 클래스는 한 역할만 한다.
- 같은 패키지 안에서만 쓰는 타입은 package-private으로 둔다.
- 모듈 간 통신은 조회면 공개 API, 상태 변화 알림이면 이벤트.
- 모듈이 커지면 패키지를 더 깊게 파지 않고 모듈을 분리한다.

## 근거

- `controller/service/repository/entity/dto/exception/config`와 `global`은 국내 실무 코드에서 가장 흔한 이름이라 바로 읽힌다.
- 오류 코드·설정·이벤트가 서비스 패키지에서 분리돼, 기능이 늘어도 서비스를 찾기 쉽다.
- Modulith가 하위 패키지를 내부로 보므로 국내형 이름을 쓰면서도 모듈 경계 검사는 그대로 유지된다.

## 검토 후 기각한 대안

- **`domain/{user,drop,...}` + `global` (최상위에 domain 패키지):** 국내에서 흔하지만 Modulith가 `domain` 하나를 모듈로 보게 돼 모듈 경계 검사가 사라진다.
- **ADR-0008 구조 유지(`internal/{web, application, domain}`):** 경계는 같지만 국내 실무와 모양이 달라 읽기 어렵고, `application`에 성격이 다른 클래스가 섞인다.

## 결과

- `user` 모듈과 `common` 모듈의 클래스 54개를 새 위치로 옮겼다(`git mv`). `common`은 `global`로 바뀌었다.
- 테스트도 대상 클래스와 같은 패키지로 옮겼다.
- 이후 `drop`, `prizecode`, `participation` 모듈도 이 구조로 만든다.
