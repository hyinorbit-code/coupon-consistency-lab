# Coupon Consistency Lab

공지된 시각에 한정 수량의 경품 코드를 선착순으로 무료 배포하는 **선착순 드롭**을 소재로 **동시성 제어, 성능 개선, 장애 복구 후의 데이터 정합성**을 직접 재현하고 측정하는 Java/Spring 백엔드 프로젝트입니다. 
부하테스트를 마친 뒤 실제 사용자 대상 이벤트로 검증합니다.

## 현재 상태

0단계(규칙·기반 결정) 완료, 1단계 구현 전입니다. 문서의 수치와 시나리오는 계획이며 실적이 아닙니다.

## 핵심 질문

| 사례 | 질문 |
| --- | --- |
| 동시성 | 여러 API 인스턴스에서도 경품 코드가 수량을 넘거나 중복 할당되지 않는가? 제어 방식별 정확성·처리량·지연은 어떻게 다른가? |
| 성능 | 오픈 시각의 순간 피크와 남은 수량 조회 폭주에서 병목은 어디이며, 변경 전후가 동일 조건에서 수치로 어떻게 달라지는가? |
| 장애 복구 | 처리 중 종료·중복 전달·컨슈머 중단 이후에도 당첨과 코드 할당이 정확히 한 번만 반영되는가? |
| 동시 회원가입 | 같은 이메일로 가입 요청이 동시에 여러 개 들어오면, 사용자가 두 명 생기거나 요청 일부가 오류로 끝나지 않는가? |

동시성·성능·장애 복구의 결론은 실전 이벤트(회차당 커피 쿠폰 10매)에서 실제 트래픽으로 검증합니다. 로그인은 이메일(인증)과 카카오로 할 수 있고, 응모는 카카오 계정이 연동된 사용자만 카카오 계정당 1회 할 수 있습니다.

### 동시 회원가입: 답변

사용자는 한 명만 생기고, 모든 요청이 같은 응답(202)을 받습니다.

1. 각 요청은 트랜잭션 안에서 이메일로 사용자를 조회하고, 없으면 INSERT합니다. 동시에 들어온 요청들은 모두 "없음"을 보고 INSERT를 시도할 수 있습니다.
2. DB의 `UNIQUE(email)` 제약이 최후 방어선입니다. 먼저 INSERT한 요청만 통과하고, 나머지는 중복 키 오류(`DataIntegrityViolationException`)로 막힙니다.
3. 막힌 요청의 트랜잭션은 롤백 전용 상태가 되어 그 안에서 다시 조회할 수 없습니다. 그래서 `TransactionTemplate`으로 **새 트랜잭션**을 열어 먼저 커밋된 사용자를 다시 조회하고, 이미 가입된 사용자로 처리합니다.
4. 이미 가입된 경우에도 응답은 새 가입과 같습니다. 응답만으로 계정 존재 여부를 알 수 없게 하기 위해서입니다. 인증 메일은 재발송 간격(1분) 안이면 다시 보내지 않아 중복 발송되지 않습니다.

애플리케이션에서 "조회 후 없으면 저장"만으로는 경쟁 조건을 막을 수 없으므로, 판정은 DB 제약에 맡기고 애플리케이션은 충돌을 복구합니다. 선착순 할당(동시성)보다 작은 규모로 같은 패턴을 먼저 적용했습니다.

## 진행 방식

작게 동작하는 버전 → 문제 재현 → 대안 비교 → 개선 → 회귀 검증 → 기록 순서로 진행합니다. 기술은 관찰한 문제가 있을 때만 도입하고, 그 근거를 ADR로 남깁니다.

모듈러 모놀리스로 시작하며, 서비스 분리는 비동기 경계가 확인된 지점에서만 선택 실험으로 다룹니다.

## AI 활용

이 프로젝트는 AI 코딩 도구(Claude Code)를 활용해 진행합니다. 목적은 백엔드 역량을 직접 쌓는 것이므로 영역마다 활용 방식을 나눴습니다.

| 영역 | 방식 |
| --- | --- |
| 백엔드 | 직접 작성합니다. AI는 설계 설명, 코드 리뷰, 버그 지적에 쓰고, 반복적인 코드 일부만 AI가 작성한 뒤 검토합니다 |
| 프론트엔드 | 백엔드에 집중하기 위해 AI로 구현했습니다. 화면 기획과 API 계약은 함께 정했습니다 |
| 문서(ADR 등) | AI가 초안을 쓰고, 결정과 검토는 직접 합니다 |

## 기술 방향

- 시작: Java 25, Spring Boot 4.1, Spring Data JPA, Spring Modulith, MySQL 8.4, Flyway, JUnit 5·Testcontainers, Docker Compose
- 배포: GitHub Actions, Amazon ECR, EC2
- 단계 도입: k6, Redis, Kafka, Outbox, (선택) Gateway·Resilience4j·분산 추적
- 선택 근거와 실행 환경은 [ADR-0001](docs/decisions/ADR-0001-initial-tech-stack.md)에 기록합니다.

## 한계

실전 이벤트는 소규모 실사용자 이벤트이며 실측값을 부풀리지 않습니다. 이벤트 실측 결과와 통제된 부하·장애 실험 결과를 구분해 기록하고, 둘 다 실서비스 운영 경력과 구분합니다. 결제와 금전 지급은 다루지 않습니다.

## 문서

- [프로젝트 계획](docs/PROJECT_PLAN.md): 배경, 목표, 범위, 업무 규칙, 아키텍처 방향, 단계별 완료 기준, 마일스톤
- [ADR-0001 초기 기술 스택](docs/decisions/ADR-0001-initial-tech-stack.md): 언어·프레임워크, DB, 실행 환경·비용, 모듈 경계 (채택)
- [ADR-0002 도메인 전환](docs/decisions/ADR-0002-domain-free-drop-event.md): 선착순 무료 드롭과 실전 이벤트 (채택)
- [ADR-0003 업무 규칙](docs/decisions/ADR-0003-business-rules.md): 참여 제한, 선착순 판정, 코드 할당, 시간, 코드 보관, 권한, 불변식 (채택)
- [ADR-0004 테스트 전략](docs/decisions/ADR-0004-test-strategy.md): 테스트 계층, 동시성 테스트, 실행·격리, 커버리지 기준 (채택)
- [ADR-0005 개발 환경](docs/decisions/ADR-0005-dev-environment.md): 맥북 개발, Windows 노트북 전체 스택 실험·부하 생성, AWS 측정 (채택)
- [ADR-0006 1단계 스키마](docs/decisions/ADR-0006-schema-v1.md): 코드 테이블로 당첨 표현, 불변식의 DB 제약, 코드 암호화·해시 분리, 코드 등록은 오픈 전만 (채택)
- [ADR-0007 인증](docs/decisions/ADR-0007-authentication.md): 무상태 JWT, 실험용 토큰 발급(experiment 프로필), 동시 첫 로그인 처리 (채택)
- [ADR-0008 패키지 구조](docs/decisions/ADR-0008-package-structure.md): 모듈은 기능 단위, 모듈 안은 web·application·domain 역할 단위 (대체됨 → ADR-0014)
- [ADR-0009 Lombok 사용 규칙](docs/decisions/ADR-0009-lombok.md): 빈은 `@RequiredArgsConstructor`, 엔티티는 `@Getter`·보호된 기본 생성자, `@Data`·`@Setter` 등은 컴파일 오류 (채택)
- [ADR-0010 로그인 수단과 응모 자격](docs/decisions/ADR-0010-login-methods-and-eligibility.md): 이메일(인증)·카카오 로그인, 응모는 카카오 연동 사용자만 (채택)
- [ADR-0011 프론트엔드](docs/decisions/ADR-0011-frontend.md): React 정적 SPA를 S3 + CloudFront로 배포, 같은 도메인에서 `/api/*`만 백엔드로 (채택)
- [ADR-0014 패키지 구조](docs/decisions/ADR-0014-package-structure-korean-convention.md): 도메인 모듈 안은 controller·service·repository·entity·dto·exception·config, 공통은 global (채택)
- [ADR-0013 오류 응답 규칙](docs/decisions/ADR-0013-error-responses.md): 모든 오류를 ProblemDetail + code로, 모듈별 ErrorCode, 전역 핸들러, 401·403 통일 (채택)
- [프론트엔드 API 계약](docs/api/FRONTEND_API_CONTRACT.md): 화면이 기대하는 요청·응답, 오류 코드, 엔드포인트별 구현·목업 상태
- [ADR-0012 이메일 회원가입과 인증](docs/decisions/ADR-0012-email-signup-and-verification.md): 계정 존재 비노출, 토큰 해시 저장, POST 인증, 커밋 후 메일 발송 (채택)

## 실행 방법

필요한 것: JDK 25(Gradle toolchain이 자동으로 찾음), Docker 런타임(Testcontainers가 MySQL 8.4 컨테이너를 띄움)

```bash
./gradlew test             # 단위·통합 테스트
./gradlew concurrencyTest  # 동시성 테스트 ("concurrency" 태그)
./gradlew check            # 전체
./gradlew bootTestRun      # Testcontainers MySQL·Mailpit과 함께 로컬 실행 (test·experiment 프로필)
```

```bash
docker exec -it $(docker ps -q --filter ancestor=mysql:8.4 | head -1) mysql --default-character-set=utf8mb4 -utest -ptest test
```

인증을 다시 구현하면 실제 환경에서는 `COUPONLAB_AUTH_JWT_SECRET`(32바이트 이상)을 환경변수로 주입해야 기동합니다. 모든 API는 `/api` 아래에 둡니다(`/actuator` 제외, ADR-0013).

프론트엔드(React + Tailwind)는 모든 화면이 만들어져 있고, 아직 백엔드에 없는 API는 목업(MSW)으로 동작합니다. 백엔드에 API가 생기는 대로 목업 대신 실제 백엔드를 쓰도록 바꿉니다. 화면 오른쪽 아래 DEV 버튼으로 사용자 전환·응모 결과 등을 바꿀 수 있습니다. 자세한 내용은 [frontend/README.md](frontend/README.md)를 참고하세요.

```bash
cd frontend && npm install && npm run dev   # http://localhost:5173, /api는 8080으로 프록시
```

배포 명령은 2단계 이후 추가합니다.
