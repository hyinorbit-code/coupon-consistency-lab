# Coupon Lab Frontend

React 정적 SPA(Vite + TypeScript)입니다. 구조와 배포 방식은 [ADR-0011](../docs/decisions/ADR-0011-frontend.md)을 따릅니다.

## 실행

Node.js 26 LTS가 필요합니다(`.nvmrc`). Homebrew의 `node@26`은 keg-only라 PATH에 직접 추가해야 합니다.

```bash
export PATH="/opt/homebrew/opt/node@26/bin:$PATH"
```

백엔드를 먼저 띄운 뒤(저장소 루트에서 `./gradlew bootTestRun`) 프론트 개발 서버를 실행합니다.

```bash
npm install
npm run dev     # http://localhost:5173
npm run build   # 타입 검사 + 프로덕션 빌드(dist/)
npm run lint    # oxlint
```

## 구조

- 개발 서버는 `/api/*` 요청을 `http://localhost:8080`으로 프록시합니다(`vite.config.ts`). 운영에서는 CloudFront가 같은 역할을 합니다.
- `src/api/`: 백엔드 API 호출. 오류 응답은 ProblemDetail의 `detail`을 메시지로 씁니다.
- `src/auth/`: 액세스 토큰을 메모리에만 보관합니다. 새로고침하면 사라지며, 리프레시 토큰 흐름은 사용자 기능 3번에서 추가합니다.
- `src/pages/`: 화면. 회원가입(`/signup`, `/signup/done`), 이메일 인증(`/verify-email`), 실험용 로그인(`/login`), 내 정보(`/me`).
