import { useEffect, useState } from 'react'
import { Navigate, useNavigate } from 'react-router'
import { fetchMe, type Me } from '../api/auth'
import { ApiError } from '../api/client'
import { useAuth } from '../auth/useAuth'

export function MePage() {
  const { token, signOut } = useAuth()
  const navigate = useNavigate()
  const [me, setMe] = useState<Me | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!token) {
      return
    }
    fetchMe(token)
      .then(setMe)
      .catch((e: unknown) => {
        if (e instanceof ApiError && e.status === 401) {
          signOut()
          return
        }
        setError(e instanceof Error ? e.message : '내 정보를 불러오지 못했습니다')
      })
  }, [token, signOut])

  if (!token) {
    return <Navigate to="/login" replace />
  }

  function handleSignOut() {
    signOut()
    navigate('/login')
  }

  return (
    <section>
      <h1>내 정보</h1>
      {error && <p className="error">{error}</p>}
      {me ? (
        <dl>
          <dt>ID</dt>
          <dd>{me.id}</dd>
          <dt>닉네임</dt>
          <dd>{me.nickname}</dd>
          <dt>역할</dt>
          <dd>{me.role}</dd>
        </dl>
      ) : (
        !error && <p>불러오는 중…</p>
      )}
      <button type="button" onClick={handleSignOut}>
        로그아웃
      </button>
    </section>
  )
}
