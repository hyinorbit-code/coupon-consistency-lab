# Coupon Consistency Lab

공지된 시각에 한정 수량의 경품 코드를 선착순으로 무료 배포하는 **선착순 드롭**을 소재로 **동시성 제어, 성능 개선, 장애 복구 후의 데이터 정합성**을 직접 재현하고 측정하는 Java/Spring 백엔드 프로젝트입니다. 통제된 실험을 마친 뒤 실제 사용자 대상 이벤트로 검증합니다.

## 현재 상태

0단계(규칙·기반 결정) 완료, 1단계 구현 전입니다(2026-10-30). 프로젝트 골격과 기반 테스트만 있고 업무 기능과 실험 결과는 아직 없습니다. 문서의 수치와 시나리오는 계획이며 실적이 아닙니다.

## 핵심 질문

| 사례 | 질문 |
| --- | --- |
| 동시성 | 여러 API 인스턴스에서도 경품 코드가 수량을 넘거나 중복 할당되지 않는가? 제어 방식별 정확성·처리량·지연은 어떻게 다른가? |
| 성능 | 오픈 시각의 순간 피크와 남은 수량 조회 폭주에서 병목은 어디이며, 변경 전후가 동일 조건에서 수치로 어떻게 달라지는가? |
| 장애 복구 | 처리 중 종료·중복 전달·컨슈머 중단 이후에도 당첨과 코드 할당이 정확히 한 번만 반영되는가? |

세 사례의 결론은 실전 이벤트(카카오 로그인, 회차당 커피 쿠폰 10매)에서 실제 트래픽으로 검증합니다.

## 진행 방식

작게 동작하는 버전 → 문제 재현 → 대안 비교 → 개선 → 회귀 검증 → 기록 순서로 진행합니다. 기술은 관찰한 문제가 있을 때만 도입하고, 그 근거를 ADR로 남깁니다.

모듈러 모놀리스로 시작하며, 서비스 분리는 비동기 경계가 확인된 지점에서만 선택 실험으로 다룹니다.

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

## 실행 방법

필요한 것: JDK 25(Gradle toolchain이 자동으로 찾음), Docker 런타임(Testcontainers가 MySQL 8.4 컨테이너를 띄움)

```bash
./gradlew test             # 단위·통합 테스트
./gradlew concurrencyTest  # 동시성 테스트 ("concurrency" 태그)
./gradlew check            # 전체
./gradlew bootTestRun      # Testcontainers MySQL과 함께 로컬 실행
```

배포 명령은 2단계 이후 추가합니다.
