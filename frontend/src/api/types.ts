/**
 * 프론트엔드가 기대하는 API 계약. 백엔드는 이 형태에 맞춰 구현한다(docs/api/FRONTEND_API_CONTRACT.md).
 * 시각은 모두 ISO-8601 UTC 문자열이다.
 */

export type Role = 'ADMIN' | 'USER'

/** 사용자 화면에서 보는 이벤트 진행 단계. 시각과 남은 수량으로 서버가 계산한다. */
export type DropPhase = 'UPCOMING' | 'OPEN' | 'SOLD_OUT' | 'CLOSED'

/** 관리자가 다루는 이벤트 상태(ADR-0006). */
export type DropStatus = 'DRAFT' | 'PUBLISHED' | 'CANCELED'

export type DropSummary = {
  id: number
  title: string
  prizeDescription: string
  openAt: string
  closeAt: string
  totalQuantity: number
  remainingQuantity: number
  phase: DropPhase
}

export type DropDetail = DropSummary & {
  /** 로그인한 사용자가 이 이벤트에서 이미 당첨됐으면 그 경품 ID. */
  myPrizeId: number | null
}

/** 남은 수량 폴링용 가벼운 응답. 4단계(성능)에서 캐시 대상이다. */
export type DropStock = {
  remainingQuantity: number
  phase: DropPhase
}

export type Prize = {
  id: number
  dropId: number
  dropTitle: string
  prizeDescription: string
  /** 복호화된 경품 코드 원문. 당첨된 본인만 받는다(ADR-0003). */
  code: string
  wonAt: string
}

export type EntryResponse = {
  result: 'WON'
  prize: Prize
}

export type Me = {
  id: number
  nickname: string
  role: Role
  email: string | null
  kakaoLinked: boolean
}

export type TokenResponse = {
  accessToken: string
  expiresAt: string
}

export type MessageResponse = {
  message: string
}

export type AdminDropSummary = {
  id: number
  title: string
  status: DropStatus
  phase: DropPhase
  openAt: string
  closeAt: string
  registeredCodes: number
  assignedCodes: number
}

export type AdminWinner = {
  nickname: string
  /** 코드 끝 4자리. 관리자 화면에서도 원문은 보여 주지 않는다. */
  codeHint: string
  assignedAt: string
}

export type AdminDropDetail = AdminDropSummary & {
  prizeDescription: string
  /** 참여 시도 수(매진 응답 포함). DB가 아니라 메트릭에서 온다(ADR-0003). */
  entryAttempts: number
  winners: AdminWinner[]
}

export type CodeUploadResult = {
  registered: number
  duplicates: number
}

/** ProblemDetail(RFC 9457)에 붙는 오류 코드(ADR-0013). 공통 코드 + 모듈별 업무 코드. */
export type ErrorCode =
  | 'INVALID_INPUT'
  | 'UNAUTHORIZED'
  | 'FORBIDDEN'
  | 'NOT_FOUND'
  | 'METHOD_NOT_ALLOWED'
  | 'CONFLICT'
  | 'INTERNAL_ERROR'
  | 'USER_NOT_FOUND'
  | 'INVALID_EMAIL'
  | 'INVALID_PASSWORD'
  | 'INVALID_VERIFICATION_TOKEN'
  | 'INVALID_CREDENTIALS'
  | 'EMAIL_NOT_VERIFIED'
  | 'ACCOUNT_LOCKED'
  | 'KAKAO_LINK_REQUIRED'
  | 'KAKAO_ALREADY_LINKED'
  | 'DROP_NOT_OPEN'
  | 'DROP_CLOSED'
  | 'SOLD_OUT'
  | 'CODES_LOCKED'
  | 'NO_CODES'
