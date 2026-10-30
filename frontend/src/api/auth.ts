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
