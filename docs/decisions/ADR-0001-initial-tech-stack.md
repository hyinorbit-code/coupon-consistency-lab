# ADR-0001 초기 기술 스택

작성일: 2026-10-30
상태: 제안 (쟁점 4개 미결정)

## 맥락

이 프로젝트는 테스트, 컨테이너·CI/CD·클라우드 배포, DB 레벨 동시성 제어, Redis, Kafka, 정량 측정이라는 갭을 메우는 것이 목적이다(PROJECT_PLAN 1절). 기술 선택 기준은 다음 세 가지다.

1. 채용 공고에서 실제로 많이 요구되는가
2. 이미 실무로 증명한 것(Spring Boot 3.5·Java 21 멀티모듈, JPA·QueryDSL)과 겹치지 않고 새로운 것을 보여주는가
3. 1차 마일스톤(1~3단계, 6~8주) 안에 소화할 수 있는가

## 제안

| 영역 | 제안 | 근거 |
| --- | --- | --- |
| 언어 | Java 25 (LTS) | Kotlin은 갭 우선순위가 낮다. 실무 21 대비 최신 LTS 대응을 보여준다 |
| 프레임워크 | Spring Boot 4.0.x | 3.5는 실무로 증명했다. Spring Framework 7, Jackson 3 등 메이저 전환 대응이 차별점이다 |
| 빌드 | Gradle (Kotlin DSL) | 업계 표준 |
| 모듈 경계 | 단일 모듈 + 패키지 분리 + Spring Modulith | 멀티모듈은 실무로 경험했다. 경계를 테스트로 검증하고, Event Publication Registry를 사례 3에서 직접 구현한 Outbox와 비교한다 |
| DB | MySQL 8.4 LTS | 국내 공고 점유율이 높고, InnoDB 락·격리 수준이 동시성 면접의 기준이다 |
| 마이그레이션 | Flyway | 스키마 이력과 재현성 |
| 데이터 접근 | Spring Data JPA, 필요한 곳만 JPQL·네이티브 쿼리 | QueryDSL은 실무 경험이 있고 포크 유지보수 이슈가 있어 필요해질 때만 도입한다 |
| 테스트 | JUnit 5, AssertJ, Testcontainers(MySQL·Redis·Kafka), Mockito 최소 사용 | H2는 락 동작이 달라 동시성 검증에 쓰지 않는다 |
| 부하 테스트 | k6 | 스크립트를 저장소에 남기고 CI와 연동하기 쉽다 |
| 관측 | Micrometer + Prometheus + Grafana, 5단계 이후 OpenTelemetry | p95/p99·처리량·커넥션 풀 지표 |
| 컨테이너 | Docker, Docker Compose | 로컬·실험 환경 일치, 다중 API 인스턴스 구성 |
| CI/CD | GitHub Actions | 테스트 → 이미지 빌드 → 레지스트리 → 배포 자동화 |
| 클라우드 | AWS: EC2 + Docker Compose, ECR (RDS는 비용 검토 후) | 공고 빈도가 가장 높다. ECS·Kubernetes는 비용 대비 갭 해소 효과가 작다 |
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

## 미결정 쟁점

1. **Spring Boot 4.0 + Java 25 vs 3.5 + Java 21**: 4.0은 차별화가 되지만 서드파티 호환 위험이 있다. 3.5는 OSS 지원이 2026년 중반에 끝났다. 제안은 4.0. 확정 전에 최신 패치 버전과 Testcontainers·springdoc·Spring Modulith 호환 여부를 확인한다.
2. **MySQL vs PostgreSQL**: 국내 범용은 MySQL, 핀테크·상위 기업 일부는 PostgreSQL.
3. **AWS 배포 형태와 비용 한도**: EC2 단일 서버로 시작할지, 처음부터 RDS를 쓸지. 월 비용 한도를 함께 정한다.
4. **Spring Modulith 채택 여부**: 경계 검증과 Outbox 비교 이점 vs 학습 대상 추가.

## 결과

쟁점이 결정되면 이 문서의 상태를 "채택"으로 바꾸고 결정 내용과 버전을 기록한다.
