import { delay, http, HttpResponse, passthrough } from 'msw'
import type { ErrorCode, Role } from '../api/types'
import * as db from './db'
import { readScenario } from './scenario'

/**
 * 아직 백엔드에 없는 API의 가짜 구현. 핸들러가 없는 요청(회원가입·이메일 인증·실험용 토큰 발급)은
 * 그대로 실제 백엔드로 간다. /users/me는 목업 토큰일 때만 여기서 처리하고, 실제 JWT면 백엔드로 넘긴다.
 */

const MOCK_TOKEN_PREFIX = 'mock-token.'
const LOCK_THRESHOLD = 5
const LOCK_MINUTES = 15

const COMMON_CODES: Record<number, ErrorCode> = {
  400: 'INVALID_INPUT',
  401: 'UNAUTHORIZED',
  403: 'FORBIDDEN',
  404: 'NOT_FOUND',
  409: 'CONFLICT',
  500: 'INTERNAL_ERROR',
}

/** 백엔드처럼 모든 오류에 code를 붙인다(ADR-0013). 업무 코드가 없으면 상태에 맞는 공통 코드. */
function problem(status: number, detail: string, code?: ErrorCode) {
  return HttpResponse.json(
    { type: 'about:blank', status, detail, code: code ?? COMMON_CODES[status] ?? 'INTERNAL_ERROR' },
    { status },
  )
}

function bearer(request: Request): string | null {
  const header = request.headers.get('Authorization')
  return header?.startsWith('Bearer ') ? header.slice('Bearer '.length) : null
}

function isMockToken(token: string | null): boolean {
  return token?.startsWith(MOCK_TOKEN_PREFIX) ?? false
}

function issueToken(user: db.MockUser) {
  return {
    accessToken: `${MOCK_TOKEN_PREFIX}${user.id}`,
    expiresAt: new Date(Date.now() + 30 * 60_000).toISOString(),
  }
}

/**
 * 요청한 사용자를 찾는다. 목업 토큰이면 목업 사용자, 실제 백엔드가 발급한 실험용 JWT면 그 sub로
 * 목업 사용자를 만들어 쓴다(실험용 계정은 응모 자격을 인정한다, ADR-0010).
 */
function currentUser(request: Request): db.MockUser | null {
  const token = bearer(request)
  if (!token) return null
  if (isMockToken(token)) {
    return db.findUser(Number(token.slice(MOCK_TOKEN_PREFIX.length))) ?? null
  }
  try {
    const payload = JSON.parse(atob(token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/'))) as {
      sub: string
      role: Role
    }
    const id = 100_000 + Number(payload.sub)
    return (
      db.findUser(id) ??
      db.registerExternalUser({ id, nickname: `실험사용자${payload.sub}`, role: payload.role })
    )
  } catch {
    return null
  }
}

async function simulateLatency() {
  await delay(readScenario().latencyMs)
}

export const handlers = [
  http.post('/api/__mock/reset', () => {
    db.resetDb()
    return new HttpResponse(null, { status: 204 })
  }),

  // ── 인증 ────────────────────────────────────────────────
  http.post('/api/auth/login', async ({ request }) => {
    await simulateLatency()
    const { email, password } = (await request.json()) as { email: string; password: string }
    const user = db.findUserByEmail(email)
    const invalid = () => problem(401, '이메일 또는 비밀번호가 올바르지 않습니다.', 'INVALID_CREDENTIALS')
    if (!user) return invalid()
    if (user.lockedUntil && user.lockedUntil > Date.now()) {
      return problem(423, `로그인 시도가 너무 많아 ${LOCK_MINUTES}분 동안 잠겼습니다.`, 'ACCOUNT_LOCKED')
    }
    if (user.password !== password) {
      user.failedLogins++
      if (user.failedLogins >= LOCK_THRESHOLD) {
        user.lockedUntil = Date.now() + LOCK_MINUTES * 60_000
        user.failedLogins = 0
        return problem(423, `로그인 시도가 너무 많아 ${LOCK_MINUTES}분 동안 잠겼습니다.`, 'ACCOUNT_LOCKED')
      }
      return invalid()
    }
    if (!user.verified) {
      return problem(403, '이메일 인증을 마친 뒤 로그인할 수 있습니다.', 'EMAIL_NOT_VERIFIED')
    }
    user.failedLogins = 0
    user.lockedUntil = null
    return HttpResponse.json(issueToken(user))
  }),

  http.post('/api/auth/logout', async () => {
    await simulateLatency()
    return new HttpResponse(null, { status: 204 })
  }),

  http.post('/api/auth/kakao/login', async () => {
    await simulateLatency()
    const user = db.findOrCreateKakaoUser()
    return HttpResponse.json(issueToken(user))
  }),

  http.get('/api/users/me', async ({ request }) => {
    if (!isMockToken(bearer(request))) {
      return passthrough()
    }
    await simulateLatency()
    const user = currentUser(request)
    return user ? HttpResponse.json(db.toMe(user)) : problem(401, '로그인이 필요합니다.')
  }),

  http.post('/api/users/me/kakao', async ({ request }) => {
    await simulateLatency()
    const user = currentUser(request)
    if (!user) return problem(401, '로그인이 필요합니다.')
    if (user.kakaoLinked) return problem(409, '이미 카카오 계정이 연동되어 있습니다.', 'KAKAO_ALREADY_LINKED')
    user.kakaoLinked = true
    return HttpResponse.json(db.toMe(user))
  }),

  http.get('/api/users/me/prizes', async ({ request }) => {
    await simulateLatency()
    const user = currentUser(request)
    return user ? HttpResponse.json(db.prizesOf(user.id)) : problem(401, '로그인이 필요합니다.')
  }),

  // ── 이벤트 ──────────────────────────────────────────────
  http.get('/api/drops', async () => {
    await simulateLatency()
    return HttpResponse.json(db.visibleDrops().map(db.toSummary))
  }),

  http.get('/api/drops/:id', async ({ request, params }) => {
    await simulateLatency()
    const drop = db.findDrop(Number(params.id))
    if (!drop || drop.status !== 'PUBLISHED') return problem(404, '이벤트를 찾을 수 없습니다.')
    const user = currentUser(request)
    return HttpResponse.json({ ...db.toSummary(drop), myPrizeId: user ? (db.prizeOf(drop, user.id)?.id ?? null) : null })
  }),

  http.get('/api/drops/:id/stock', async ({ params }) => {
    const drop = db.findDrop(Number(params.id))
    if (!drop || drop.status !== 'PUBLISHED') return problem(404, '이벤트를 찾을 수 없습니다.')
    if (readScenario().simulateCrowd && db.phaseOf(drop) === 'OPEN' && Math.random() < 0.35) {
      db.simulateOtherEntry(drop)
    }
    return HttpResponse.json({ remainingQuantity: db.remaining(drop), phase: db.phaseOf(drop) })
  }),

  http.post('/api/drops/:id/entries', async ({ request, params }) => {
    await simulateLatency()
    const user = currentUser(request)
    if (!user) return problem(401, '로그인이 필요합니다.')
    const drop = db.findDrop(Number(params.id))
    if (!drop || drop.status !== 'PUBLISHED') return problem(404, '이벤트를 찾을 수 없습니다.')
    drop.entryAttempts++

    const existing = db.prizeOf(drop, user.id)
    if (existing) return HttpResponse.json({ result: 'WON', prize: existing })
    if (!user.kakaoLinked) {
      return problem(403, '카카오 계정을 연동한 사용자만 응모할 수 있습니다.', 'KAKAO_LINK_REQUIRED')
    }
    const phase = db.phaseOf(drop)
    if (phase === 'UPCOMING') return problem(409, '아직 응모가 시작되지 않았습니다.', 'DROP_NOT_OPEN')
    if (phase === 'CLOSED') return problem(409, '응모 기간이 끝났습니다.', 'DROP_CLOSED')

    const { entryOutcome } = readScenario()
    if (entryOutcome === 'server-error') return problem(500, '서버에 문제가 생겼습니다. 잠시 후 다시 시도해 주세요.')
    const prize = entryOutcome === 'sold-out' ? null : db.assignCode(drop, user.id)
    if (!prize) return problem(409, '아쉽지만 준비된 경품이 모두 소진되었습니다.', 'SOLD_OUT')
    return HttpResponse.json({ result: 'WON', prize }, { status: 201 })
  }),

  // ── 관리자 ──────────────────────────────────────────────
  ...adminHandlers(),
]

function requireAdmin(request: Request) {
  const user = currentUser(request)
  if (!user) return problem(401, '로그인이 필요합니다.')
  if (user.role !== 'ADMIN') return problem(403, '관리자만 접근할 수 있습니다.')
  return null
}

function adminHandlers() {
  return [
    http.get('/api/admin/drops', async ({ request }) => {
      await simulateLatency()
      return requireAdmin(request) ?? HttpResponse.json(db.allDrops().map(db.toAdminSummary))
    }),

    http.get('/api/admin/drops/:id', async ({ request, params }) => {
      await simulateLatency()
      const denied = requireAdmin(request)
      if (denied) return denied
      const drop = db.findDrop(Number(params.id))
      return drop ? HttpResponse.json(db.toAdminDetail(drop)) : problem(404, '이벤트를 찾을 수 없습니다.')
    }),

    http.post('/api/admin/drops', async ({ request }) => {
      await simulateLatency()
      const denied = requireAdmin(request)
      if (denied) return denied
      const body = (await request.json()) as { title: string; prizeDescription: string; openAt: string; closeAt: string }
      const openAt = Date.parse(body.openAt)
      const closeAt = Date.parse(body.closeAt)
      if (!(openAt < closeAt)) return problem(400, '종료 시각은 오픈 시각보다 늦어야 합니다.')
      const drop = db.createDrop({ title: body.title, prizeDescription: body.prizeDescription, openAt, closeAt })
      return HttpResponse.json(db.toAdminDetail(drop), { status: 201 })
    }),

    http.post('/api/admin/drops/:id/codes', async ({ request, params }) => {
      await simulateLatency()
      const denied = requireAdmin(request)
      if (denied) return denied
      const drop = db.findDrop(Number(params.id))
      if (!drop) return problem(404, '이벤트를 찾을 수 없습니다.')
      if (drop.status === 'CANCELED' || Date.now() >= drop.openAt) {
        return problem(409, '오픈 이후에는 코드를 등록할 수 없습니다.', 'CODES_LOCKED')
      }
      const { codes } = (await request.json()) as { codes: string[] }
      return HttpResponse.json(db.addCodes(drop, codes))
    }),

    http.post('/api/admin/drops/:id/publish', async ({ request, params }) => {
      await simulateLatency()
      const denied = requireAdmin(request)
      if (denied) return denied
      const drop = db.findDrop(Number(params.id))
      if (!drop) return problem(404, '이벤트를 찾을 수 없습니다.')
      if (drop.codes.length === 0) return problem(409, '코드를 하나 이상 등록해야 공개할 수 있습니다.', 'NO_CODES')
      drop.status = 'PUBLISHED'
      return HttpResponse.json(db.toAdminDetail(drop))
    }),

    http.post('/api/admin/drops/:id/cancel', async ({ request, params }) => {
      await simulateLatency()
      const denied = requireAdmin(request)
      if (denied) return denied
      const drop = db.findDrop(Number(params.id))
      if (!drop) return problem(404, '이벤트를 찾을 수 없습니다.')
      drop.status = 'CANCELED'
      return HttpResponse.json(db.toAdminDetail(drop))
    }),
  ]
}
