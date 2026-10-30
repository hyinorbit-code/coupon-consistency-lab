# ADR-0009 Lombok 사용 규칙

작성일: 2026-10-30
상태: 채택

## 맥락

처음에는 의존성을 최소로 두려고 Lombok 없이 빈 생성자와 엔티티 getter를 직접 작성했다. 국내 실무 코드 대부분이 Lombok을 쓰고 반복 코드가 줄어드는 이점이 있어 도입한다. 다만 엔티티에 `@Data`·`@Setter`·`@ToString`·`@EqualsAndHashCode`를 쓰면 불변식이 깨지거나 지연 로딩·순환 참조 문제가 생기기 쉽다.

## 결정

1. Lombok을 도입한다. 버전은 Spring Boot가 관리하는 값(2026-10-30 기준 1.18.48, JDK 25 지원)을 쓴다.
2. 허용하는 기능:

| 대상 | 사용 |
| --- | --- |
| 스프링 빈(컨트롤러, 서비스, 컴포넌트) | `@RequiredArgsConstructor` + `private final` 필드로 생성자 주입 |
| JPA 엔티티 | `@Getter`, `@NoArgsConstructor(access = AccessLevel.PROTECTED)` |
| DTO·설정·값 | Lombok 대신 `record` |

3. 금지하는 기능: `@Data`, `@Setter`, `@ToString`, `@EqualsAndHashCode`. 루트 `lombok.config`에서 `flagUsage = error`로 지정해 쓰면 컴파일이 실패한다.
4. 엔티티 상태 변경은 setter가 아니라 의도를 드러내는 메서드(예: 나중에 `User.withdraw()`)로 한다. 생성은 검증을 거치는 정적 팩토리(`User.create()`)로 한다.
5. 생성 코드에 `@lombok.Generated`를 붙여(`lombok.addLombokGeneratedAnnotation`) JaCoCo 커버리지 집계에서 제외한다.

## 근거

- `@RequiredArgsConstructor`는 직접 쓴 생성자 주입과 동작이 같고(단일 생성자 자동 주입), 불변성·명시적 의존성을 유지한다. 필드 주입(`@Autowired` 필드)은 쓰지 않는다.
- 금지 규칙을 문서가 아니라 컴파일 오류로 강제해 리뷰에서 놓치지 않게 한다.

## 결과

- `user` 모듈의 빈 생성자 4곳(`UserController`, `ExperimentAuthController`, `UserService`, `JwtTokenIssuer`)과 `User` 엔티티의 getter·기본 생성자를 Lombok으로 바꿨다.
- 새 JDK로 올릴 때는 Lombok 호환 버전을 먼저 확인한다.
