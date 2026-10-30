import { apiRequest } from './client'

export type Role = 'ADMIN' | 'USER'

export type TokenResponse = {
  accessToken: string
  expiresAt: string
}

export type Me = {
  id: number
  nickname: string
  role: Role
}

/** experiment 프로필에서만 있는 실험용 토큰 발급(ADR-0007). */
export function issueExperimentToken(providerUserId: string, nickname: string, role: Role) {
  return apiRequest<TokenResponse>('/experiment/auth/token', {
    method: 'POST',
    body: { providerUserId, nickname, role },
  })
}

export function fetchMe(token: string) {
  return apiRequest<Me>('/users/me', { token })
}

export type MessageResponse = {
  message: string
}

/** 이메일 회원가입. 계정 존재 여부와 관계없이 같은 응답을 받는다(ADR-0012). */
export function signUp(email: string, password: string, nickname: string) {
  return apiRequest<MessageResponse>('/auth/signup', {
    method: 'POST',
    body: { email, password, nickname },
  })
}

export function verifyEmail(token: string) {
  return apiRequest<MessageResponse>('/auth/verify-email', {
    method: 'POST',
    body: { token },
  })
}

export function resendVerificationEmail(email: string) {
  return apiRequest<MessageResponse>('/auth/verify-email/resend', {
    method: 'POST',
    body: { email },
  })
}
