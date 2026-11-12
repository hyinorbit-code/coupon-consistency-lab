import { apiRequest } from './client'
import type { Me, MessageResponse, Role, TokenResponse } from './types'

/** experiment 프로필에서만 있는 실험용 토큰 발급. 실제 백엔드로 간다. */
export function issueExperimentToken(providerUserId: string, nickname: string, role: Role) {
  return apiRequest<TokenResponse>('/experiment/auth/token', {
    method: 'POST',
    body: { providerUserId, nickname, role },
  })
}

export function fetchMe(token: string) {
  return apiRequest<Me>('/users/me', { token })
}

/** 이메일 회원가입. 계정 존재 여부와 관계없이 같은 응답을 받는다. 실제 백엔드로 간다. */
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

/** 이메일 로그인. 아직 목업(MSW)이다. */
export function login(email: string, password: string) {
  return apiRequest<TokenResponse>('/auth/login', {
    method: 'POST',
    body: { email, password },
  })
}

export function logout(token: string) {
  return apiRequest<void>('/auth/logout', { method: 'POST', token })
}

/** 카카오 인가 코드로 로그인(없으면 가입). 아직 목업(MSW)이다. */
export function loginWithKakao(code: string) {
  return apiRequest<TokenResponse>('/auth/kakao/login', {
    method: 'POST',
    body: { code },
  })
}

/** 로그인한 사용자에 카카오 계정을 연동한다. 아직 목업(MSW)이다. */
export function linkKakao(token: string, code: string) {
  return apiRequest<Me>('/users/me/kakao', {
    method: 'POST',
    token,
    body: { code },
  })
}
