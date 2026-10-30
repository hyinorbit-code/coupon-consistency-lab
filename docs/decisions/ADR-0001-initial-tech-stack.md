# ADR-0001 초기 기술 스택

작성일: 2026-10-30
상태: 채택 (2026-10-30, 쟁점 4개 결정). 쟁점 3의 개발 환경은 ADR-0005로 대체됨

## 맥락

이 프로젝트는 테스트, 컨테이너·CI/CD·클라우드 배포, DB 레벨 동시성 제어, Redis, Kafka, 정량 측정이라는 갭을 메우는 것이 목적이다(PROJECT_PLAN 1절). 기술 선택 기준은 다음 세 가지다.

1. 채용 공고에서 실제로 많이 요구되는가
2. 이미 실무로 증명한 것(Spring Boot 3.5·Java 21 멀티모듈, JPA·QueryDSL)과 겹치지 않고 새로운 것을 보여주는가
3. 1차 마일스톤(1~3단계, 6~8주) 안에 소화할 수 있는가

## 제안

| 영역 | 제안 | 근거 |
| --- | --- | --- |
| 언어 | **(결정)** Java 25 (LTS), Eclipse Temurin | Kotlin은 갭 우선순위가 낮다. 실무 21 대비 최신 LTS 대응을 보여준다 |
| 프레임워크 | **(결정)** Spring Boot 4.1.x | 3.5는 실무로 증명했다. Spring Framework 7, Jackson 3 등 메이저 전환 대응이 차별점이다 |
| 빌드 | Gradle (Kotlin DSL) | 업계 표준 |
| 모듈 경계 | **(결정)** 단일 모듈 + 패키지 분리 + Spring Modulith 제한 채택 | 멀티모듈은 실무로 경험했다. 경계를 테스트로 검증하고, Event Publication Registry를 사례 3에서 직접 구현한 Outbox와 비교한다 |
| DB | **(결정)** MySQL 8.4 LTS | 국내 공고 점유율이 높고, InnoDB 락·격리 수준이 동시성 면접의 기준이다 |
| 마이그레이션 | Flyway | 스키마 이력과 재현성 |
| 데이터 접근 | Spring Data JPA, 필요한 곳만 JPQL·네이티브 쿼리 | QueryDSL은 실무 경험이 있고 포크 유지보수 이슈가 있어 필요해질 때만 도입한다 |
| 테스트 | JUnit 5, AssertJ, Testcontainers(MySQL·Redis·Kafka), Mockito 최소 사용 | H2는 락 동작이 달라 동시성 검증에 쓰지 않는다 |
| 부하 테스트 | k6 | 스크립트를 저장소에 남기고 CI와 연동하기 쉽다 |
| 관측 | Micrometer + Prometheus + Grafana, 5단계 이후 OpenTelemetry | p95/p99·처리량·커넥션 풀 지표 |
| 컨테이너 | Docker, Docker Compose | 로컬·실험 환경 일치, 다중 API 인스턴스 구성 |
| CI/CD | GitHub Actions | 테스트 → 이미지 빌드 → 레지스트리 → 배포 자동화 |
| 실행 환경 | **(결정)** 로컬 노트북 + AWS(EC2 + Docker Compose, ECR), 공식 성능 측정은 비버스터블 EC2를 실험 때만 사용 | 쟁점 3 참고 |
| 캐시·분산 제어 | Redis + Lettuce(Lua 원자 연산), Redisson은 분산 락 비교용 | 3~4단계에서 DB 방식과 비교한다 |
| 메시징 | Apache Kafka (KRaft) + Spring Kafka, Outbox 직접 구현(폴링), Debezium은 선택 | 5단계 |
| API 문서 | springdoc-openapi | |
| 인증 | Spring Security + 단순 JWT 또는 실험용 헤더 인증 | 인증 플랫폼은 범위 밖 |

### 단계별 도입 시점

| 단계 | 도입 |
| --- | --- |
| 0~1 | Java, Spring Boot, Gradle, MySQL, Flyway, JPA, Spring Modulith, JUnit, Testcontainers, Docker Compose |
| 2 | GitHub Actions, ECR, EC2, Prometheus, Grafana |
| 3~4 | k6, Redis |
| 5~6 | Kafka, Outbox, OpenTelemetry |

## 결정된 쟁점

### 1. Spring Boot·Java 버전 → Spring Boot 4.1.x + Java 25 (2026-10-30)

2026-10-30 기준 확인한 사실:

| 버전 | 출시 | OSS 지원 종료 | 최신 패치 |
| --- | --- | --- | --- |
| Spring Boot 4.1 | 2026-06 | 2027-07 | 4.1.2 |
| Spring Boot 4.0 | 2025-11 | 2026-12 | 4.0.9 |
| Spring Boot 3.5 | 2025-05 | 2026-06 (종료, 이후 상용 지원만) | 3.5.16 |
| Java 25 LTS | 2025-09 | Premier 2030-09 | 25.0.5 |
| Java 21 LTS | 2023-09 | Premier 2028-09 | 21.0.13 |

- 호환: springdoc-openapi 3.2.0이 Boot 4.1.2 기반으로 출시됐다. Testcontainers 2.0.x가 나와 있으며 2.0부터 아티팩트 이름과 패키지가 바뀌었다(예: `org.testcontainers:mysql` → `testcontainers-mysql`). Spring Modulith 2.x는 Boot 4 기준 라인이다.
- 기각: Boot 3.5 + Java 21은 OSS 지원이 종료됐고 실무와 겹쳐 차별화가 없다. Boot 4.0은 OSS 지원이 두 달 남아 4.1보다 나은 점이 없다.
- 감수할 비용: Jackson 3 전환, 스타터 이름 변경, `@MockBean` → `@MockitoBean` 등 Boot 4 변경점 학습. 서드파티 호환 문제가 생기면 이 ADR에 기록한다.
- 구현 시작일에 4.1.x 최신 패치를 다시 확인해 고정한다.
- Java 25의 Virtual Thread는 4단계(성능)의 선택 실험 소재로 둔다.

### 2. DB → MySQL 8.4 LTS (2026-10-30)

2026-10-30 기준 확인한 사실:

| 버전 | 종류 | 출시 | 지원 종료 | AWS RDS |
| --- | --- | --- | --- | --- |
| MySQL 9.7 | LTS | 2026-04 | Premier 2031-04 | 프리뷰 환경만 (60일 후 자동 삭제) |
| MySQL 8.4 | LTS | 2024-04 | Premier 2029-04 | 정식 지원 |
| PostgreSQL 18 | 최신 메이저 | 2025-09 | 2030-11 | 정식 지원 |

- 근거: 목표가 업종 무관 중견 이상이므로 국내 공고 빈도가 가장 높은 MySQL을 택한다. InnoDB 레코드 락·갭 락·넥스트 키 락과 `REPEATABLE READ` 기본 동작을 재현·측정하는 것이 사례 1의 깊이가 되고, 실무 MariaDB/MySQL의 DeadLock 해결 경험과 직접 이어진다.
- 8.4를 고른 이유: 9.7은 RDS 정식 지원 전이고, 이 프로젝트에서 쓰는 InnoDB 락·격리 동작은 8.4와 같아 얻는 것이 거의 없다.
- 기각: PostgreSQL은 `SKIP LOCKED` 기반 Outbox 폴링과 논리 복제 CDC가 깔끔하지만, 목표 회사군을 핀테크·PostgreSQL 사용 기업으로 좁힐 때 재검토한다.

### 3. 실행 환경과 비용 한도 → 로컬 노트북 + AWS 최소 상시 구성 + 실험 시점 비버스터블 인스턴스 (2026-10-30)

| 역할 | 위치 | 비용 |
| --- | --- | --- |
| 개발, 테스트, 기능·동시성 정확성·장애 실험 | 로컬 노트북 (Windows, 메모리 32GB 이상, WSL2 + Docker) | 무료 |
| 상시 배포 (CI/CD, 롤백 검증) | EC2 t4g.small 1대 + Docker Compose(앱 + MySQL 8.4), GitHub Actions → ECR(arm64 이미지) → SSM 배포 | 월 약 $20 (추정) |
| 공식 성능 측정 | 실험 때만 띄우는 비버스터블 EC2(c7g·m7g 계열)와 같은 리전의 별도 부하 생성기 인스턴스, 종료 후 삭제 | 실험당 수 달러 (추정) |
| 관리형 DB | 필요한 실험에서만 RDS db.t4g.micro를 띄운다 | 사용 시간만 |

- 비용 통제: AWS Budgets 알림 상시 $30 / 전체 $50. NAT Gateway, 상시 ALB, 미사용 Elastic IP를 두지 않는다. 클라우드 실험이 반복되면 Terraform 도입을 검토한다.
- 측정 원칙: 이력서·사례 문서의 수치는 비버스터블 인스턴스에서 측정한다. 버스터블(t 계열)은 CPU 크레딧 잔량에 따라 성능이 달라지고, 노트북은 발열 스로틀링·하이브리드 코어·WSL2 계층·부하 생성기 동거로 노이즈가 크다. 노트북 성능 결과는 경향 확인용으로만 쓴다(전원 연결, 성능 모드 고정, 반복 측정 후 중앙값).
- 기각: 회사 소유 온프레미스 서버는 사양은 충분하지만 회사 자산에 개인 프로젝트를 올리는 것이 보안 규정·이직 준비 성격과 충돌하고, 퇴사 후 재현할 수 없어 쓰지 않는다. ECS Fargate + ALB는 1차 마일스톤에 비해 비용과 복잡도가 크다.
- 확인 필요: AWS 계정 생성 시점(2025-07-15 전후)에 따라 프리티어 방식이 다르다. 새 계정은 크레딧 방식이며 Free 플랜은 6개월 후 닫히므로 Paid 플랜으로 시작한다. 요금은 구성 시 콘솔에서 확인한다.

### 4. Spring Modulith → 제한 채택 (2026-10-30)

- 1단계부터 `ApplicationModules.verify()`로 모듈 간 의존 규칙을 테스트에서 강제하고, `@ApplicationModuleTest`로 모듈 단위 통합 테스트를 작성한다.
- 5단계 Outbox의 핵심(DB 변경과 이벤트 기록, Kafka 발행, 멱등 소비)은 직접 구현한다. Event Publication Registry는 직접 구현과 실패 처리·재발행·운영 편의성을 비교하는 실험에만 쓴다.
- 근거: 실무 멀티모듈 경계 설계를 "테스트로 강제하는 경계"로 확장하고, "모듈러 모놀리스 경계를 어떻게 지켰고 왜 분리하지 않았나"에 근거를 남긴다. 사례 3에 비교 대상이 하나 생긴다.
- 감수할 점: 공고 키워드로서의 가치는 낮다. 최상위 패키지를 모듈로 보는 관례를 따라야 한다. Registry에 의존하면 Outbox 직접 구현 경험이 흐려지므로 사용 범위를 위와 같이 제한한다.
- 기각: 패키지 + ArchUnit은 경계 강제 효과가 비슷하지만 규칙을 직접 작성해야 하고 사례 3 비교 대상이 없다. 패키지 규칙만 두는 방식은 경계가 쉽게 무너진다.
- Boot 4.1에 맞는 Modulith 2.x 버전은 구현 시작일에 확인해 고정한다.

## 결과

- 0~1단계 구현은 이 ADR의 스택으로 시작한다. 구현 시작일에 Spring Boot 4.1.x, Spring Modulith 2.x, Testcontainers 2.0.x, springdoc-openapi 3.x의 최신 패치를 확인해 빌드 파일에 고정한다.
- 결정을 바꿀 때는 이 문서를 수정하지 않고 새 ADR로 대체하며, 이 문서 상태를 "대체됨"으로 바꾼다.
