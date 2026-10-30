# ADR-0013 오류 응답 규칙: ProblemDetail + code, 전역 핸들러

작성일: 2026-10-30
상태: 채택
변경: 2026-10-30 `common` 모듈은 `global`로 이름과 위치가 바뀜(ADR-0014). `global.error`에 ErrorCode·BusinessException·GlobalExceptionHandler, `global.security`에 SecurityProblemHandlers

## 맥락

프론트엔드 API 계약에서 "모든 오류는 ProblemDetail(RFC 9457), 화면이 분기할 업무 오류는 `code` 속성"으로 정했다. 그런데 백엔드는 `user` 모듈 전용 예외 핸들러와 `@ResponseStatus`가 섞여 있었고, 응답에 `code`가 없었으며, Spring Security 필터가 내보내는 401·403은 본문 형식이 달랐다. 모듈이 늘기 전에 한 곳에서 처리 규칙을 정한다.

## 결정

### 1. 공통 모듈 `common`

| 타입 | 위치 | 역할 |
| --- | --- | --- |
| `ErrorCode` | `common` (공개) | 업무 오류 인터페이스: `code()`(enum 이름), `status()`, `defaultMessage()` |
| `CommonErrorCode` | `common` (공개) | 모듈에 속하지 않는 공통 오류 |
| `BusinessException` | `common` (공개) | `ErrorCode`를 담는 업무 예외의 부모. 메시지는 사용자에게 그대로 보인다 |
| `SecurityProblemHandlers` | `common` (공개) | 보안 필터의 401·403을 같은 형식으로 쓰는 진입점·거부 핸들러 |
| `GlobalExceptionHandler` | `common.internal.web` | 모든 모듈 공통 `@RestControllerAdvice` |

`common`은 다른 모듈을 참조하지 않고, 모든 모듈이 `common`의 공개 타입을 참조할 수 있다.

### 2. 오류 코드는 모듈마다 enum으로

각 모듈이 자기 오류를 `ErrorCode`를 구현한 enum으로 소유한다(예: `UserErrorCode.USER_NOT_FOUND`, 이후 `participation` 모듈의 `SOLD_OUT`). 모든 코드를 한 enum에 모으면 모든 모듈이 그 파일에 의존하게 되기 때문이다. 자주 쓰는 오류는 이름 있는 하위 클래스로 둔다(`UserNotFoundException extends BusinessException`).

### 3. 처리 규칙

| 예외 | 상태 | code |
| --- | --- | --- |
| `BusinessException` | ErrorCode가 정함 | ErrorCode 이름 |
| `@Valid` 검증 실패 | 400 | `INVALID_INPUT` + `errors: [{field, message}]` |
| 잘못된 JSON·타입 불일치 등 MVC 표준 4xx | 상태대로 | 대응하는 공통 코드(없으면 `INVALID_INPUT`) |
| `IllegalArgumentException`(도메인 값 검증) | 400 | `INVALID_INPUT`. 개발자용 메시지는 응답에 넣지 않는다 |
| 인증 없음·잘못된 토큰(보안 필터) | 401 | `UNAUTHORIZED` + `WWW-Authenticate` 헤더 |
| 권한 없음(보안 필터) | 403 | `FORBIDDEN` |
| 없는 경로 | 404 | `NOT_FOUND` |
| 지원하지 않는 메서드 | 405 | `METHOD_NOT_ALLOWED` |
| 그 외 모든 예외 | 500 | `INTERNAL_ERROR`. 스택 트레이스는 로그에만, 응답은 일반 메시지만 |

응답 예:

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "입력값을 확인해 주세요.",
  "instance": "/api/auth/signup",
  "code": "INVALID_INPUT",
  "errors": [{ "field": "email", "message": "올바른 형식의 이메일 주소여야 합니다" }]
}
```

### 4. 구현 메모

- `GlobalExceptionHandler`는 `ResponseEntityExceptionHandler`를 상속해 MVC 표준 예외 처리를 재사용하고, `handleExceptionInternal`에서 code를 덧붙인다. 일부 표준 예외(없는 경로 등)는 본문을 `null`로 넘기므로 예외가 가진 ProblemDetail(`ErrorResponse.getBody()`)을 먼저 꺼낸다.
- 백엔드는 API만 제공하므로 `spring.web.resources.add-mappings=false`로 정적 리소스 매핑을 끈다. 켜 두면 없는 경로가 정적 리소스 핸들러로 빠져 전역 핸들러를 거치지 않는다.
- Spring 7의 `ProblemDetail`은 `type` 기본값이 비어 있어, 보안 필터 응답에는 `about:blank`를 직접 넣는다.
- 보안 필터 응답은 Jackson 3 `JsonMapper`로 쓴다.

## 검증

`ErrorResponseTests`: 검증 실패(필드별 오류), 잘못된 JSON, 업무 예외, 401(토큰 없음·변조, `WWW-Authenticate` 포함), 403(일반 사용자의 관리자 API), 404, `IllegalArgumentException`(내부 메시지 비노출), 500(예외 메시지·스택 비노출), 직접 메시지를 준 `BusinessException`. 이 테스트에서만 오류 발생용 엔드포인트를 등록한다.

## 검토 후 기각한 대안

- **모듈별 예외 핸들러:** 모듈이 늘수록 응답 형식이 갈라진다.
- **오류 코드를 한 enum에 모으기:** 모든 모듈이 하나의 파일에 의존하고, 모듈 경계를 흐린다.
- **`@ResponseStatus` 예외:** code 속성을 붙일 수 없고 처리 방식이 두 가지가 된다.

## 결과

- `UserApiExceptionHandler`를 지우고, `UserNotFoundException`·`InvalidVerificationTokenException`을 `BusinessException` 기반으로 바꿨다.
- 프론트 API 계약의 오류 코드 표에 공통 코드를 추가했다.
