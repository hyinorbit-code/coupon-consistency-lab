# ADR-0012 이메일 회원가입과 인증

작성일: 2026-10-30
상태: 채택

## 맥락

ADR-0010에서 이메일·비밀번호 로그인을 추가하고, 이메일 가입자는 인증을 마쳐야 로그인할 수 있게 했다. 가입·인증 API는 인증 없이 열려 있으므로 계정 존재 여부 노출, 토큰 탈취, 메일 폭탄, 동시 가입을 고려해야 한다.

## 결정

### API

| 메서드 | 경로 | 응답 |
| --- | --- | --- |
| POST | `/api/auth/signup` `{email, password, nickname}` | 항상 202 + 같은 안내 문구 |
| POST | `/api/auth/verify-email` `{token}` | 200, 토큰이 없거나 만료·사용됐으면 400(구분하지 않음) |
| POST | `/api/auth/verify-email/resend` `{email}` | 항상 202 + 같은 안내 문구 |

### 1. 계정 존재 여부를 응답으로 드러내지 않는다
- 새 이메일: 인증 대기 사용자를 만들고 인증 메일을 보낸다.
- 인증 대기 중인 이메일: 재발송 간격(1분)이 지났으면 새 인증 메일을 보낸다. 비밀번호·닉네임은 바꾸지 않는다.
- 이미 인증된 이메일: "이미 가입된 계정" 안내 메일만 보낸다. 기존 정보는 바꾸지 않는다.
- 응답은 세 경우 모두 같다. 재발송 API도 계정이 없거나 이미 인증됐으면 조용히 아무것도 하지 않는다.

### 2. 인증 토큰
- 32바이트 `SecureRandom` 값을 URL-safe Base64로 만들어 메일로만 보낸다. DB에는 SHA-256 해시만 저장한다.
- 유효기간 24시간(`couponlab.signup.verification-ttl`), 한 번만 사용한다.
- 사용 처리는 `UPDATE … SET used_at = ? WHERE id = ? AND used_at IS NULL` 조건부 UPDATE로 해서 같은 토큰의 동시 요청 중 하나만 성공한다.

### 3. 인증은 GET이 아니라 POST
메일 보안 스캐너가 링크를 미리 열어도 토큰이 소진되지 않도록, 메일 링크(`{couponlab.web.base-url}/verify-email?token=…`)는 프론트 페이지를 가리키고 그 페이지가 `POST`로 인증한다. 개발 모드 React StrictMode가 effect를 두 번 실행하므로 프론트는 요청을 한 번만 보내도록 막는다.

### 4. 메일은 커밋 후에 보낸다
- 가입 트랜잭션 안에서는 모듈 내부 이벤트(`VerificationMailRequested`, `AlreadyRegisteredMailRequested`)만 발행하고, `@TransactionalEventListener(AFTER_COMMIT)`에서 메일을 보낸다. 롤백되면 메일도 나가지 않는다.
- 발송이 실패해도 가입은 유지되고(로그만 남김), 사용자는 재발송으로 복구한다.
- DB 변경과 외부 부수효과를 분리하는 문제의 작은 버전이며, 5단계 Outbox에서 유실 가능성(커밋 직후 프로세스 종료 시 메일 미발송)까지 다룬다.

### 5. 비밀번호
- 8~64자. `PasswordEncoderFactories.createDelegatingPasswordEncoder()`(기본 BCrypt)로 해시하며 `{bcrypt}` 접두사가 붙어 이후 알고리즘을 바꿔도 기존 해시를 검증할 수 있다.
- BCrypt는 의도적으로 느리므로 DB 트랜잭션 밖에서 해시한다.

### 6. 이메일 정규화: `EmailAddress` 값 객체
- 이메일은 `EmailAddress` 값 객체(record)로 다룬다. 생성될 때 스스로 앞뒤 공백 제거·소문자 변환·기본 형식 검사를 하고, 그 뒤로는 바뀌지 않는다. 그래서 이 타입이 있으면 항상 정규화가 끝난 값이다.
- 리포지토리(`findByEmail(EmailAddress)`)와 엔티티(`User.signUpWithEmail(EmailAddress, ...)`)가 이 타입만 받으므로, 정규화하지 않은 문자열로 조회·저장하는 실수(예: 로그인에서 정규화 누락)가 컴파일 단계에서 막힌다.
- DB 컬럼은 문자열 하나 그대로이며 JPA `AttributeConverter`로 변환한다.
- 형식 오류는 `InvalidEmailException`(`INVALID_EMAIL`, 400). 요청 DTO의 `@Email`이 먼저 걸러 내고, 값 객체는 서비스 입구의 최종 방어선이다.
- 값을 바꾸는 메서드를 두지 않는다. 정규화 전 상태가 존재하면 같은 실수가 다시 생기고, 값 객체의 동등성(`equals`/`hashCode`)과 JPA 변경 감지가 흔들리기 때문이다.
- `+` 별칭이나 `.` 제거는 하지 않는다. 이메일은 응모 자격이 아니므로(ADR-0010) 그 정도 정규화가 필요 없다.

### 7. 동시 가입
같은 이메일의 동시 가입은 `UNIQUE(email)`이 하나만 통과시킨다. 진 쪽은 새 트랜잭션에서 먼저 커밋된 사용자를 기존 사용자로 처리하고 같은 202 응답을 받는다.

### 8. 메일 발송 구성
- `VerificationMailSender` 인터페이스(application)와 SMTP 구현(`mail` 패키지). SMTP 구현은 `spring.mail.host`가 있을 때만 등록된다.
- 로컬(`bootTestRun`): Testcontainers로 Mailpit을 함께 띄우고 웹 화면 주소를 콘솔에 출력한다.
- 테스트: 메일을 메모리에 모으는 가짜 발송기(`CapturingMailSender`)로 발송 여부와 링크를 검증한다.

## 검증

- `SignupIntegrationTests`: 가입(인증 대기, BCrypt 해시, 인증 메일), 인증 성공과 토큰 재사용 거절, 만료·없는 토큰, 이미 인증된 이메일 재가입(안내 메일만, 기존 정보 유지), 재발송 간격, 이메일 정규화, 입력 검증, 없는 이메일 재발송, 롤백 시 메일 미발송.
- `SignupConcurrencyTests`: 같은 이메일 20개 동시 가입 시 사용자 1명, 모든 요청 정상 종료(9건이 유니크 제약에 막혔다가 복구).
- 브라우저 + Mailpit: 가입 → 실제 메일 수신 → 링크로 인증(POST 1회) → 같은 링크 재사용 시 실패 안내, 이미 가입된 이메일 재가입 시 안내 메일 수신.

## 알려진 한계

- 이미 인증된 이메일로 반복 가입하면 안내 메일이 매번 나간다. 남의 이메일로 메일 폭탄을 보낼 여지가 있어 E단계의 요청 속도 제한에서 함께 막는다.
- 재발송 간격 검사와 토큰 발급 사이에 동시 요청이 끼면 토큰이 두 개 생길 수 있다. 둘 다 유효하고 각각 한 번만 쓸 수 있어 정합성 문제는 없다.
- **동시 가입 중 데드락 가능성(2026-10-31 기록, 대응 보류).** 같은 이메일로 3개 이상 요청이 동시에 들어오고, 먼저 INSERT한 트랜잭션이 커밋하지 않고 **롤백**하면, 그 키를 기다리던 나머지 요청들 사이에 InnoDB 데드락(MySQL 1213)이 날 수 있다. 이 경우 Spring은 중복 키 오류(`DataIntegrityViolationException`)가 아니라 락 획득 실패 계열 예외(`CannotAcquireLockException` 등)를 던지므로 지금의 `catch`에 걸리지 않고 500(`INTERNAL_ERROR`)이 된다. 발생 조건이 좁고(첫 요청의 트랜잭션 실패 + 3개 이상 동시) 사용자는 다시 시도하면 되므로 받아들인다. 필요해지면 이 예외에 짧은 재시도 1회를 넣는다. 같은 패턴인 `UserService.findOrCreate`(ADR-0007)에도 같은 한계가 있다.
