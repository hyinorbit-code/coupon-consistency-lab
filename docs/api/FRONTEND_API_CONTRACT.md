# 프론트엔드 API 계약

작성일: 2026-10-30
근거: 프론트엔드 목업(`frontend/src/api/`, `frontend/src/mocks/handlers.ts`)

프론트엔드를 먼저 전부 만들고, 아직 없는 API는 MSW 핸들러로 흉내 냈다. 이 문서는 화면이 기대하는 요청·응답 형태이며 백엔드는 이 계약에 맞춰 구현한다. 타입 정의의 원본은 `frontend/src/api/types.ts`다.

## 공통 규칙

- 모든 경로는 `/api` 아래에 있다(`ApiPathConfiguration`).
- 시각은 ISO-8601 UTC 문자열이다. 화면이 한국 시간으로 바꿔 표시한다.
- 인증은 `Authorization: Bearer <accessToken>`.
- 오류는 ProblemDetail(RFC 9457)이며, **모든 오류 응답에 `code` 속성**이 붙는다(ADR-0013). 입력 검증 실패는 `errors: [{field, message}]`도 함께 온다.

```json
{ "type": "about:blank", "status": 409, "detail": "아쉽지만 준비된 경품이 모두 소진되었습니다.", "code": "SOLD_OUT" }
```

공통 코드(모든 API):

| code | status | 의미 |
| --- | --- | --- |
| `INVALID_INPUT` | 400 | 입력 검증 실패, 잘못된 JSON 등 |
| `UNAUTHORIZED` | 401 | 토큰 없음·만료·변조 |
| `FORBIDDEN` | 403 | 권한 없음(예: 일반 사용자의 관리자 API) |
| `NOT_FOUND` | 404 | 없는 경로·대상 |
| `METHOD_NOT_ALLOWED` | 405 | 지원하지 않는 메서드 |
| `CONFLICT` | 409 | 현재 상태와 충돌 |
| `INTERNAL_ERROR` | 500 | 서버 오류(내부 정보는 응답에 없음) |

업무 코드(구현됨):

| code | status | 의미 |
| --- | --- | --- |
| `USER_NOT_FOUND` | 404 | 사용자 없음 |
| `INVALID_PASSWORD` | 400 | 비밀번호 정책 위반(공백뿐, 8~64자 아님). `detail`에 구체적인 이유 |
| `INVALID_VERIFICATION_TOKEN` | 400 | 인증 토큰이 없거나 만료·사용됨(구분하지 않음) |

업무 코드(목업, 백엔드 구현 예정):

| code | status | 의미 |
| --- | --- | --- |
| `INVALID_CREDENTIALS` | 401 | 이메일 또는 비밀번호 불일치(계정 존재 여부를 구분하지 않음) |
| `EMAIL_NOT_VERIFIED` | 403 | 이메일 인증 전 로그인 |
| `ACCOUNT_LOCKED` | 423 | 로그인 실패 누적으로 잠김 |
| `KAKAO_LINK_REQUIRED` | 403 | 카카오 미연동 사용자의 응모(ADR-0010) |
| `KAKAO_ALREADY_LINKED` | 409 | 이미 카카오가 연동된 사용자 |
| `DROP_NOT_OPEN` | 409 | 오픈 전 응모 |
| `DROP_CLOSED` | 409 | 종료 후 응모 |
| `SOLD_OUT` | 409 | 매진 |
| `CODES_LOCKED` | 409 | 오픈 이후·취소된 이벤트에 코드 등록(ADR-0006) |
| `NO_CODES` | 409 | 코드 없이 공개 시도 |

## 엔드포인트

상태: **구현** = 실제 백엔드, **목업** = MSW만 있음, **보완 필요** = 백엔드에 있지만 계약과 다름.

### 인증·계정

| 메서드 | 경로 | 요청 | 응답 | 상태 |
| --- | --- | --- | --- | --- |
| POST | `/auth/signup` | `{email, password, nickname}` | 202 `{message}` | 구현 |
| POST | `/auth/verify-email` | `{token}` | 200 `{message}` | 구현 |
| POST | `/auth/verify-email/resend` | `{email}` | 202 `{message}` | 구현 |
| POST | `/auth/login` | `{email, password}` | 200 `{accessToken, expiresAt}` | 목업 (사용자 기능 2번) |
| POST | `/auth/logout` | – | 204 | 목업 (사용자 기능 3번) |
| POST | `/auth/kakao/login` | `{code}` | 200 `{accessToken, expiresAt}` | 목업 (사용자 기능 4번) |
| POST | `/experiment/auth/token` | `{providerUserId, nickname, role}` | 200 `{accessToken, expiresAt}` | 구현 (experiment 프로필) |
| GET | `/users/me` | – | 200 `Me` | **보완 필요**: `email`, `kakaoLinked` 추가 |
| POST | `/users/me/kakao` | `{code}` | 200 `Me` | 목업 (사용자 기능 4번) |
| GET | `/users/me/prizes` | – | 200 `Prize[]` | 목업 (1단계 당첨 조회) |

`Me = {id, nickname, role, email: string | null, kakaoLinked: boolean}`

### 이벤트(사용자)

| 메서드 | 경로 | 응답 | 상태 |
| --- | --- | --- | --- |
| GET | `/drops` | `DropSummary[]` (공개된 이벤트만) | 목업 |
| GET | `/drops/{id}` | `DropDetail` (로그인 시 `myPrizeId` 포함) | 목업 |
| GET | `/drops/{id}/stock` | `{remainingQuantity, phase}` | 목업. 화면이 2초마다 폴링하며 4단계 캐시 대상 |
| POST | `/drops/{id}/entries` | 201 `{result: "WON", prize}`. 이미 당첨이면 200으로 같은 경품(멱등) | 목업 (1단계 핵심) |

- `DropSummary = {id, title, prizeDescription, openAt, closeAt, totalQuantity, remainingQuantity, phase}`
- `phase`: `UPCOMING`(오픈 전) · `OPEN` · `SOLD_OUT` · `CLOSED`. 서버가 DB 시각과 남은 수량으로 계산한다(ADR-0003).
- `Prize = {id, dropId, dropTitle, prizeDescription, code, wonAt}`. `code`는 복호화된 원문이며 본인에게만 준다.
- 응모 처리 순서: 미인증 401 → 이미 당첨이면 그 경품 → 카카오 미연동 403 → 오픈 전·종료 409 → 할당(매진이면 409).

### 관리자 (`ADMIN`만)

| 메서드 | 경로 | 요청 | 응답 | 상태 |
| --- | --- | --- | --- | --- |
| GET | `/admin/drops` | – | `AdminDropSummary[]` | 목업 |
| GET | `/admin/drops/{id}` | – | `AdminDropDetail` | 목업 |
| POST | `/admin/drops` | `{title, prizeDescription, openAt, closeAt}` | 201 `AdminDropDetail` (DRAFT) | 목업 |
| POST | `/admin/drops/{id}/codes` | `{codes: string[]}` | 200 `{registered, duplicates}` | 목업 |
| POST | `/admin/drops/{id}/publish` | – | 200 `AdminDropDetail` | 목업 |
| POST | `/admin/drops/{id}/cancel` | – | 200 `AdminDropDetail` | 목업 |

- `AdminDropSummary = {id, title, status, phase, openAt, closeAt, registeredCodes, assignedCodes}`
- `AdminDropDetail = AdminDropSummary + {prizeDescription, entryAttempts, winners: [{nickname, codeHint, assignedAt}]}`
- `entryAttempts`는 매진 응답을 포함한 참여 시도 수로, DB가 아니라 메트릭에서 온다(ADR-0003).
- `codeHint`는 코드 끝 4자리. 관리자 화면에도 원문은 보내지 않는다.

## 목업에서 정한 화면 규칙(백엔드와 맞춰야 할 것)

- 매진·종료 응답은 참여 기록을 남기지 않는다(ADR-0003). 목업은 `entryAttempts`만 올린다.
- 코드 등록은 오픈 전에만 가능하고, 중복 코드는 제외하고 개수를 돌려준다.
- 로그인 실패 5회면 15분 잠금(목업 값). 사용자 기능 2번에서 확정한다.
- 카카오 로그인은 프론트가 인가 코드를 받아 `POST /auth/kakao/login`으로 넘기는 형태로 그렸다. Spring Security OAuth2 Client로 서버에서 콜백을 처리하게 되면 이 계약을 바꾼다(사용자 기능 4번).
