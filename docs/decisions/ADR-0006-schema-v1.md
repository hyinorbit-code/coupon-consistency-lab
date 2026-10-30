# ADR-0006 1단계 스키마

작성일: 2026-10-30
상태: 채택

## 맥락

ADR-0003의 업무 규칙과 불변식을 1단계에서 구현할 스키마로 옮긴다. 규칙을 가능한 한 DB 제약으로 강제해, 동시성 제어가 실패해도 DB가 최후 방어선이 되게 한다.

## 결정

### 1. 참여 기록을 별도 테이블로 두지 않는다

ADR-0003에서 매진·종료 응답은 기록하지 않기로 했으므로 참여 기록은 당첨자 기록이고, 당첨자 기록은 코드 할당과 같은 사실이다. 당첨은 `prize_codes.assigned_user_id`가 채워진 행으로 표현한다. ADR-0003의 "참여 기록"은 "할당된 코드 행"을 뜻하는 것으로 바꿔 읽는다.

- 같은 사실을 두 곳에 저장하지 않으므로 불변식 4(당첨 기록과 코드 할당은 함께 존재)가 구조적으로 항상 참이 된다.
- 5단계에서 참여 접수를 비동기로 바꾸면 그때 접수 테이블을 추가한다.

### 2. 테이블

| 테이블 | 역할 | 주요 제약 |
| --- | --- | --- |
| `users` | 카카오 사용자와 실험용 사용자 | `UNIQUE(auth_provider, provider_user_id)` |
| `drop_events` | 드롭 이벤트 | `CHECK(open_at < close_at)` |
| `prize_codes` | 경품 코드와 할당(당첨) | `UNIQUE(code_hash)`, `UNIQUE(drop_event_id, assigned_user_id)`, 사용자·할당 시각 동시 존재 `CHECK` |

DDL은 `src/main/resources/db/migration/V1__create_users_drop_events_prize_codes.sql`에 있다.

### 3. 불변식과 강제 방법

| 불변식 | 강제 방법 |
| --- | --- |
| 1. 할당된 코드 수 ≤ 등록된 코드 수 | 코드 행 자체가 수량이다. 같은 행을 두 번 UPDATE하는 덮어쓰기는 동시성 제어로 막아야 하며 3단계 재현 대상이다 |
| 2. 코드 하나는 최대 한 명에게 | `assigned_user_id`가 단일 컬럼이다 |
| 3. 사용자당 이벤트별 당첨 최대 1회 | `UNIQUE(drop_event_id, assigned_user_id)`. MySQL은 유니크 인덱스에서 NULL 중복을 허용하므로 미할당 코드는 여러 개 둘 수 있다 |
| 4. 당첨과 할당은 함께 존재 | 같은 행이다. `CHECK((assigned_user_id IS NULL) = (assigned_at IS NULL))` |
| 5. 재처리·재시작 전후 당첨자 집합 불변 | 할당 해제 경로가 없다(ADR-0003: 당첨 취소·코드 반환 없음) |

검증: `SchemaConstraintTests`가 Testcontainers MySQL 8.4에서 위 제약을 확인한다. MySQL의 CHECK 위반(에러 3819)은 Spring이 `DataIntegrityViolationException`으로 변환하지 않고 `UncategorizedSQLException`으로 던진다. 따라서 CHECK는 최후 방어선으로 두고, 기간·할당 규칙은 애플리케이션에서 먼저 검증해 의미 있는 오류로 응답한다.

### 4. 경품 코드 암호화와 중복 방지 분리

- `code_ciphertext`: AES-GCM(IV + 암호문). IV가 매번 달라 같은 코드도 암호문이 다르다.
- `code_hash`: 별도 키로 만든 HMAC-SHA256. 결정적이므로 유니크 제약으로 중복 등록을 막는다.
- `code_hint`: 끝 4자리. 관리자 화면 마스킹용.
- 기밀성(무작위 암호화)과 중복 검사(결정적 해시)에 서로 다른 키를 쓴다. 두 키 모두 환경변수로 주입하고 저장소에 넣지 않는다.

### 5. 시간

- 모든 시각은 `DATETIME(6)`에 UTC로 저장한다. `TIMESTAMP`는 세션 시간대에 따른 자동 변환이 끼어들어 쓰지 않는다.
- 오픈 판정 쿼리는 `UTC_TIMESTAMP(6)`을 명시해 세션 시간대 설정에 영향받지 않게 한다.

### 6. 이벤트 상태와 코드 등록 (ADR-0003에 추가하는 규칙)

- 상태: `DRAFT`(코드 등록) → `PUBLISHED`(공개) → 필요 시 `CANCELED`.
- **코드 등록은 오픈 전에만 허용한다.** 오픈 뒤 수량이 바뀌면 선착순 공정성이 깨진다.
- 진행 중·종료 여부는 상태가 아니라 시각으로 판정한다. 상태를 바꾸는 배치가 필요 없다.

### 7. 미할당 코드 조회 인덱스

`(drop_event_id, assigned_user_id, id)`는 `WHERE drop_event_id = ? AND assigned_user_id IS NULL ORDER BY id LIMIT 1` 경로용이다. 3단계의 비관적 락·`SKIP LOCKED` 실험이 이 경로를 쓴다.

## 지금 만들지 않는 테이블과 컬럼

스키마 변경은 Flyway 마이그레이션으로 이력과 함께 관리하므로, 필요가 확인된 시점에 추가한다. 아직 정하지 않은 결정에 기대 미리 만든 테이블은 실제 요구와 어긋나기 쉽다.

| 대상 | 추가 시점 | 근거가 될 결정 |
| --- | --- | --- |
| `prize_codes.version` (낙관적 락) | 3단계 | 동시성 제어 방식 비교 실험 |
| 참여 접수 테이블 | 5단계 | 비동기 접수 전환 여부와 상태 계약 |
| Outbox 테이블 | 5단계 | Outbox 직접 구현 |
| Spring Modulith `event_publication` | 5단계 | Event Publication Registry 비교 실험 |

E단계(실전 이벤트) 이후 실제 데이터가 생기면 운영 중 스키마 변경은 하위 호환을 유지하는 확장 → 이전 → 축소(expand-contract) 순서로 한다.
