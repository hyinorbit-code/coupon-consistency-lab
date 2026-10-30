import { useMemo, useState, type ReactNode } from 'react'
import { AuthContext, type AuthState } from './authContext'

/**
 * 액세스 토큰을 메모리에만 보관한다. 새로고침하면 사라지며, 리프레시 토큰(HttpOnly 쿠키)으로
 * 다시 발급받는 흐름은 사용자 기능 3번에서 추가한다(ADR-0011).
 */
export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(null)

  const value = useMemo<AuthState>(
    () => ({ token, signIn: setToken, signOut: () => setToken(null) }),
    [token],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
