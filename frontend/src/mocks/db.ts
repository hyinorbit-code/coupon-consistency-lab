import type {
  AdminDropDetail,
  AdminDropSummary,
  DropPhase,
  DropStatus,
  DropSummary,
  Me,
  Prize,
  Role,
} from '../api/types'

/**
 * MSW가 쓰는 목 DB. 새로고침해도 유지되도록 localStorage에 저장하고, 개발 패널에서 초기화하면 처음 상태로 돌아간다.
 * 실제 백엔드 규칙(ADR-0003, ADR-0006, ADR-0010)을 흉내 낸다.
 */

export type MockUser = {
  id: number
  email: string
  password: string
  nickname: string
  role: Role
  verified: boolean
  kakaoLinked: boolean
  failedLogins: number
  lockedUntil: number | null
}

type MockCode = {
  id: number
  code: string
  assignedUserId: number | null
  assignedAt: number | null
}

export type MockDrop = {
  id: number
  title: string
  prizeDescription: string
  openAt: number
  closeAt: number
  status: DropStatus
  codes: MockCode[]
  entryAttempts: number
}

/** 목업 로그인 계정. 실제 백엔드에는 없는 값이며 비밀번호는 모두 같다. */
export const MOCK_PASSWORD = 'password1234'

export const MOCK_ACCOUNTS = {
  user: 'user@example.com',
  noKakao: 'nokakao@example.com',
  admin: 'admin@example.com',
  pending: 'pending@example.com',
} as const

const MINUTE = 60_000
const HOUR = 60 * MINUTE

let nextId = 1000

function generateCode(): string {
  const part = () => Math.random().toString(36).slice(2, 6).toUpperCase().padEnd(4, '0')
  return `CPN-${part()}-${part()}-${part()}`
}

function codes(total: number, assigned: number, userIds: number[] = []): MockCode[] {
  return Array.from({ length: total }, (_, index) => ({
    id: nextId++,
    code: generateCode(),
    assignedUserId: index < assigned ? (userIds[index] ?? 9000 + index) : null,
    assignedAt: index < assigned ? Date.now() - (assigned - index) * 7_000 : null,
  }))
}

function createState() {
  const now = Date.now()
  const users: MockUser[] = [
    { id: 1, email: MOCK_ACCOUNTS.user, password: MOCK_PASSWORD, nickname: '커피러버', role: 'USER', verified: true, kakaoLinked: true, failedLogins: 0, lockedUntil: null },
    { id: 2, email: MOCK_ACCOUNTS.noKakao, password: MOCK_PASSWORD, nickname: '연동전사용자', role: 'USER', verified: true, kakaoLinked: false, failedLogins: 0, lockedUntil: null },
    { id: 3, email: MOCK_ACCOUNTS.admin, password: MOCK_PASSWORD, nickname: '운영자', role: 'ADMIN', verified: true, kakaoLinked: true, failedLogins: 0, lockedUntil: null },
    { id: 4, email: MOCK_ACCOUNTS.pending, password: MOCK_PASSWORD, nickname: '인증대기', role: 'USER', verified: false, kakaoLinked: false, failedLogins: 0, lockedUntil: null },
  ]
  const drops: MockDrop[] = [
    {
      id: 1,
      title: '오늘의 커피 선착순 10명',
      prizeDescription: '편의점 커피 2,000원 교환권',
      openAt: now - 2 * MINUTE,
      closeAt: now + HOUR,
      status: 'PUBLISHED',
      codes: codes(10, 6),
      entryAttempts: 184,
    },
    {
      id: 2,
      title: '곧 열려요! 아메리카노 드롭',
      prizeDescription: '편의점 아메리카노 2,000원 교환권',
      openAt: now + 90 * 1000,
      closeAt: now + 2 * HOUR,
      status: 'PUBLISHED',
      codes: codes(10, 0),
      entryAttempts: 0,
    },
    {
      id: 3,
      title: '내일 저녁 8시 정각 드롭',
      prizeDescription: '편의점 커피 2,000원 교환권',
      openAt: nextKstTime(20, 1),
      closeAt: nextKstTime(20, 1) + 2 * HOUR,
      status: 'PUBLISHED',
      codes: codes(10, 0),
      entryAttempts: 0,
    },
    {
      id: 4,
      title: '1회차 리허설 드롭',
      prizeDescription: '편의점 커피 2,000원 교환권',
      openAt: now - 3 * HOUR,
      closeAt: now + 3 * HOUR,
      status: 'PUBLISHED',
      codes: codes(10, 10, [1]),
      entryAttempts: 1532,
    },
    {
      id: 5,
      title: '지난주 커피 드롭',
      prizeDescription: '편의점 커피 2,000원 교환권',
      openAt: now - 7 * 24 * HOUR,
      closeAt: now - 7 * 24 * HOUR + 2 * HOUR,
      status: 'PUBLISHED',
      codes: codes(10, 10),
      entryAttempts: 3210,
    },
    {
      id: 6,
      title: '준비 중인 다음 드롭',
      prizeDescription: '편의점 커피 2,000원 교환권',
      openAt: nextKstTime(20, 3),
      closeAt: nextKstTime(20, 3) + 2 * HOUR,
      status: 'DRAFT',
      codes: [],
      entryAttempts: 0,
    },
  ]
  return { users, drops }
}

/** 오늘부터 daysAhead일 뒤 KST hour시 정각의 epoch ms. */
function nextKstTime(hour: number, daysAhead: number): number {
  const kstOffset = 9 * HOUR
  const kstNow = new Date(Date.now() + kstOffset)
  const target = Date.UTC(kstNow.getUTCFullYear(), kstNow.getUTCMonth(), kstNow.getUTCDate() + daysAhead, hour)
  return target - kstOffset
}

const STORAGE_KEY = 'coupon-lab:mock-db'

type State = ReturnType<typeof createState>

function loadState(): State | null {
  try {
    const stored = localStorage.getItem(STORAGE_KEY)
    if (!stored) return null
    const parsed = JSON.parse(stored) as { state: State; nextId: number }
    nextId = parsed.nextId
    return parsed.state
  } catch {
    return null
  }
}

let state: State = loadState() ?? createState()

/** 핸들러가 상태를 바꾼 뒤 호출한다(mockFetch). */
export function persistDb() {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify({ state, nextId }))
  } catch {
    // 저장할 수 없으면 메모리에서만 유지한다.
  }
}

export function resetDb() {
  state = createState()
  persistDb()
}

export function findUserByEmail(email: string) {
  return state.users.find((user) => user.email === email.trim().toLowerCase())
}

export function findUser(id: number) {
  return state.users.find((user) => user.id === id)
}

export function findDrop(id: number) {
  return state.drops.find((drop) => drop.id === id)
}

export function visibleDrops() {
  return state.drops.filter((drop) => drop.status === 'PUBLISHED')
}

export function allDrops() {
  return [...state.drops].sort((a, b) => b.openAt - a.openAt)
}

export function remaining(drop: MockDrop) {
  return drop.codes.filter((code) => code.assignedUserId === null).length
}

export function phaseOf(drop: MockDrop, now = Date.now()): DropPhase {
  if (now < drop.openAt) return 'UPCOMING'
  if (now >= drop.closeAt) return 'CLOSED'
  if (remaining(drop) === 0) return 'SOLD_OUT'
  return 'OPEN'
}

export function prizeOf(drop: MockDrop, userId: number): Prize | null {
  const code = drop.codes.find((candidate) => candidate.assignedUserId === userId)
  if (!code || code.assignedAt === null) return null
  return {
    id: code.id,
    dropId: drop.id,
    dropTitle: drop.title,
    prizeDescription: drop.prizeDescription,
    code: code.code,
    wonAt: new Date(code.assignedAt).toISOString(),
  }
}

export function prizesOf(userId: number): Prize[] {
  return state.drops
    .map((drop) => prizeOf(drop, userId))
    .filter((prize): prize is Prize => prize !== null)
    .sort((a, b) => b.wonAt.localeCompare(a.wonAt))
}

/** 미할당 코드 하나를 사용자에게 할당한다. 실제 백엔드에서는 동시성 제어가 필요한 지점이다(3단계). */
export function assignCode(drop: MockDrop, userId: number): Prize | null {
  const code = drop.codes.find((candidate) => candidate.assignedUserId === null)
  if (!code) return null
  code.assignedUserId = userId
  code.assignedAt = Date.now()
  return prizeOf(drop, userId)
}

/** 다른 참여자가 코드를 가져간 것처럼 하나를 할당한다(개발 패널의 참여자 시뮬레이션). */
export function simulateOtherEntry(drop: MockDrop) {
  assignCode(drop, 9000 + Math.floor(Math.random() * 1000))
  drop.entryAttempts += 1 + Math.floor(Math.random() * 30)
}

/** 실제 백엔드가 발급한 실험용 JWT 사용자를 목업 사용자로 등록한다. */
export function registerExternalUser(input: { id: number; nickname: string; role: Role }) {
  const user: MockUser = {
    ...input,
    email: `experiment-${input.id}@local`,
    password: '',
    verified: true,
    kakaoLinked: true,
    failedLogins: 0,
    lockedUntil: null,
  }
  state.users.push(user)
  return user
}

/** 카카오 로그인 목업: 항상 같은 카카오 사용자로 로그인한다(없으면 만든다). */
export function findOrCreateKakaoUser() {
  const existing = state.users.find((user) => user.nickname === '카카오사용자')
  if (existing) return existing
  const user: MockUser = {
    id: nextId++,
    email: '',
    password: '',
    nickname: '카카오사용자',
    role: 'USER',
    verified: true,
    kakaoLinked: true,
    failedLogins: 0,
    lockedUntil: null,
  }
  state.users.push(user)
  return user
}

export function createDrop(input: { title: string; prizeDescription: string; openAt: number; closeAt: number }) {
  const drop: MockDrop = { id: nextId++, ...input, status: 'DRAFT', codes: [], entryAttempts: 0 }
  state.drops.push(drop)
  return drop
}

export function addCodes(drop: MockDrop, rawCodes: string[]) {
  const existing = new Set(state.drops.flatMap((candidate) => candidate.codes.map((code) => code.code)))
  let registered = 0
  let duplicates = 0
  for (const raw of rawCodes) {
    const value = raw.trim()
    if (!value) continue
    if (existing.has(value)) {
      duplicates++
      continue
    }
    existing.add(value)
    drop.codes.push({ id: nextId++, code: value, assignedUserId: null, assignedAt: null })
    registered++
  }
  return { registered, duplicates }
}

export function toMe(user: MockUser): Me {
  return { id: user.id, nickname: user.nickname, role: user.role, email: user.email || null, kakaoLinked: user.kakaoLinked }
}

export function toSummary(drop: MockDrop): DropSummary {
  return {
    id: drop.id,
    title: drop.title,
    prizeDescription: drop.prizeDescription,
    openAt: new Date(drop.openAt).toISOString(),
    closeAt: new Date(drop.closeAt).toISOString(),
    totalQuantity: drop.codes.length,
    remainingQuantity: remaining(drop),
    phase: phaseOf(drop),
  }
}

export function toAdminSummary(drop: MockDrop): AdminDropSummary {
  return {
    id: drop.id,
    title: drop.title,
    status: drop.status,
    phase: phaseOf(drop),
    openAt: new Date(drop.openAt).toISOString(),
    closeAt: new Date(drop.closeAt).toISOString(),
    registeredCodes: drop.codes.length,
    assignedCodes: drop.codes.length - remaining(drop),
  }
}

export function toAdminDetail(drop: MockDrop): AdminDropDetail {
  return {
    ...toAdminSummary(drop),
    prizeDescription: drop.prizeDescription,
    entryAttempts: drop.entryAttempts,
    winners: drop.codes
      .filter((code) => code.assignedUserId !== null && code.assignedAt !== null)
      .sort((a, b) => (b.assignedAt ?? 0) - (a.assignedAt ?? 0))
      .map((code) => ({
        nickname: findUser(code.assignedUserId!)?.nickname ?? `참여자${code.assignedUserId}`,
        codeHint: code.code.slice(-4),
        assignedAt: new Date(code.assignedAt!).toISOString(),
      })),
  }
}
