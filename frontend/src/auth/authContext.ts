import { createContext } from 'react'
import type { Me } from '../api/types'

export type AuthState = {
  token: string | null
  me: Me | null
  /** 저장된 세션으로 내 정보를 다시 불러오는 중(목업 모드 새로고침 직후). */
  restoring: boolean
  /** 토큰을 받은 뒤 내 정보를 불러온다. 실패하면 로그아웃 상태로 남는다. */
  signIn: (token: string) => Promise<Me>
  signOut: () => void
  /** 카카오 연동처럼 내 정보가 바뀐 뒤 다시 불러온다. */
  setMe: (me: Me) => void
}

export const AuthContext = createContext<AuthState | null>(null)
