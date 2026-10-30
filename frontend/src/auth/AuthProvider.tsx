import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import { fetchMe, logout } from '../api/auth'
import type { Me } from '../api/types'
import { mocksEnabled } from '../mocks/enabled'
import { AuthContext, type AuthState } from './authContext'

/** 목업 모드에서만 새로고침해도 로그인을 유지하려고 sessionStorage에 토큰을 둔다. */
const MOCK_SESSION_KEY = 'coupon-lab:mock-session-token'
const persistSession = import.meta.env.DEV && mocksEnabled

function readSessionToken(): string | null {
  if (!persistSession) return null
  try {
    return sessionStorage.getItem(MOCK_SESSION_KEY)
  } catch {
    return null
  }
}

function writeSessionToken(token: string | null) {
  if (!persistSession) return
  try {
    if (token) sessionStorage.setItem(MOCK_SESSION_KEY, token)
    else sessionStorage.removeItem(MOCK_SESSION_KEY)
  } catch {
    // 저장소를 쓸 수 없으면 메모리에서만 유지한다.
  }
}

function normalize(me: Me): Me {
  return { ...me, email: me.email ?? null, kakaoLinked: me.kakaoLinked ?? false }
}

/**
 * 액세스 토큰을 메모리에만 보관한다. 새로고침하면 사라지며, 리프레시 토큰(HttpOnly 쿠키)으로
 * 다시 발급받는 흐름은 사용자 기능 3번에서 추가한다(ADR-0011). 목업 모드에서만 예외로 sessionStorage를 쓴다.
 */
export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(readSessionToken)
  const [me, setMe] = useState<Me | null>(null)
  const [restoring, setRestoring] = useState(() => readSessionToken() !== null)

  // 목업 모드에서 새로고침한 경우 저장된 토큰으로 내 정보를 다시 불러온다.
  useEffect(() => {
    const stored = readSessionToken()
    if (!stored) return
    fetchMe(stored)
      .then((loaded) => setMe(normalize(loaded)))
      .catch(() => {
        writeSessionToken(null)
        setToken(null)
      })
      .finally(() => setRestoring(false))
  }, [])

  const signIn = useCallback(async (nextToken: string) => {
    const loaded = normalize(await fetchMe(nextToken))
    setToken(nextToken)
    setMe(loaded)
    writeSessionToken(nextToken)
    return loaded
  }, [])

  const signOut = useCallback(() => {
    if (token) {
      logout(token).catch(() => undefined)
    }
    setToken(null)
    setMe(null)
    writeSessionToken(null)
  }, [token])

  const value = useMemo<AuthState>(
    () => ({ token, me, restoring, signIn, signOut, setMe }),
    [token, me, restoring, signIn, signOut],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
