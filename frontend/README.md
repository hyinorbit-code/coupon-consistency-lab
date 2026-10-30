# Coupon Lab Frontend

React 정적 SPA(Vite + TypeScript + Tailwind CSS v4)입니다. 구조와 배포 방식은 [ADR-0011](../docs/decisions/ADR-0011-frontend.md), 화면이 기대하는 API는 [프론트엔드 API 계약](../docs/api/FRONTEND_API_CONTRACT.md)을 따릅니다.

## 실행

Node.js 26 LTS가 필요합니다(`.nvmrc`). Homebrew의 `node@26`은 keg-only라 PATH에 직접 추가해야 합니다.

```bash
export PATH="/opt/homebrew/opt/node@26/bin:$PATH"
```

```bash
npm install
npm run dev     # http://localhost:5173
npm run build   # 타입 검사 + 프로덕션 빌드(dist/)
npm run lint    # oxlint
```

회원가입·이메일 인증·실험용 로그인은 실제 백엔드를 쓰므로, 이 화면들을 볼 때는 저장소 루트에서 `./gradlew bootTestRun`을 먼저 실행합니다. 나머지 화면은 백엔드 없이도 목업으로 동작합니다.

## 목업 모드 (개발 서버 기본값)

아직 백엔드에 없는 API는 MSW 핸들러(`src/mocks/handlers.ts`)가 가짜 응답을 줍니다. 서비스 워커 대신 API 클라이언트가 핸들러를 직접 호출하므로(`src/mocks/mockFetch.ts`) 서비스 워커를 쓸 수 없는 내장 브라우저에서도 동작합니다. 핸들러가 없는 요청은 Vite 프록시를 거쳐 실제 백엔드로 갑니다.

- 목업 응답은 네트워크 탭에 보이지 않고 콘솔에 `[mock] GET /api/drops → 200`처럼 남습니다.
- 목 데이터는 localStorage에, 목업 로그인은 sessionStorage에 저장돼 새로고침해도 유지됩니다(목업 모드 전용).
- 목업을 끄려면 `VITE_USE_MOCKS=false npm run dev`. 운영 빌드에는 MSW와 개발 패널이 포함되지 않습니다.

화면 오른쪽 아래 **DEV** 버튼으로 개발 패널을 엽니다.

| 기능 | 설명 |
| --- | --- |
| 사용자 전환 | 일반 사용자(카카오 연동) / 카카오 미연동 / 관리자 / 로그아웃 |
| 다음 응모 결과 | 실제 남은 수량대로 / 강제 매진 / 서버 오류(500) |
| 참여자 시뮬레이션 | 진행 중 이벤트의 남은 수량이 폴링 때마다 줄어듦 |
| 응답 지연 | 모든 목업 응답에 지연 추가 |
| 목 데이터 초기화 | 처음 상태로 되돌림 |

목업 계정(비밀번호는 모두 `password1234`): `user@example.com`(카카오 연동), `nokakao@example.com`, `admin@example.com`, `pending@example.com`(인증 대기). 같은 계정으로 5번 틀리면 잠깁니다.

## 화면

| 경로 | 화면 | API |
| --- | --- | --- |
| `/` | 이벤트 목록(진행 중·오픈 예정·종료) | 목업 |
| `/drops/:id` | 이벤트 상세(카운트다운, 남은 수량 폴링, 상태별 응모 버튼) | 목업 |
| `/drops/:id/result` | 응모 결과(당첨 바코드·코드 복사·카카오톡 공유 / 매진) | 목업 |
| `/my/prizes` | 내 당첨 내역 | 목업 |
| `/login` | 이메일 로그인, 카카오로 시작하기 | 목업 |
| `/signup`, `/signup/done`, `/verify-email` | 회원가입, 메일 확인·재발송, 이메일 인증 | **실제 백엔드** |
| `/auth/kakao/callback` | 카카오 로그인·연동 콜백 | 목업 |
| `/my` | 마이페이지(카카오 연동, 로그아웃) | 목업 + 실제 |
| `/terms`, `/privacy` | 이용약관, 개인정보 처리방침(초안) | – |
| `/experiment-login` | 실제 백엔드 실험용 로그인(개발 패널에서 진입) | **실제 백엔드** |
| `/admin`, `/admin/drops/new`, `/admin/drops/:id` | 관리자: 이벤트 목록·생성·상세(코드 등록, 공개·취소, 당첨자, 대사) | 목업 |

## 구조

- `src/api/`: API 호출과 계약 타입(`types.ts`). 오류는 ProblemDetail의 `detail`·`code`를 씁니다.
- `src/auth/`: 액세스 토큰을 메모리에 보관합니다. 리프레시 토큰 흐름은 사용자 기능 3번에서 추가합니다.
- `src/components/`: 레이아웃, 공통 UI(`ui.tsx`), 이벤트 UI(`drop.tsx`), 가드, 개발 패널.
- `src/mocks/`: 목 DB, 시나리오 설정, MSW 핸들러.
- `src/pages/`: 화면. 관리자 화면은 `pages/admin/`.
