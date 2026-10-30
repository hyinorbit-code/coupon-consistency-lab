import type { ReactNode } from 'react'
import { Navigate, useLocation } from 'react-router'
import { useAuth } from '../auth/useAuth'
import { Alert, LinkButton, LoadingBlock } from './ui'

/** 로그인하지 않았으면 로그인 화면으로 보내고, 로그인 후 원래 화면으로 돌아오게 한다. */
export function RequireAuth({ children }: { children: ReactNode }) {
  const { me, restoring } = useAuth()
  const location = useLocation()
  if (restoring) return <LoadingBlock />
  if (!me) {
    return <Navigate to={`/login?next=${encodeURIComponent(location.pathname)}`} replace />
  }
  return children
}

export function RequireAdmin({ children }: { children: ReactNode }) {
  const { me, restoring } = useAuth()
  const location = useLocation()
  if (restoring) return <LoadingBlock />
  if (!me) {
    return <Navigate to={`/login?next=${encodeURIComponent(location.pathname)}`} replace />
  }
  if (me.role !== 'ADMIN') {
    return (
      <div className="mx-auto max-w-md space-y-4 px-4 py-16">
        <Alert kind="error">관리자만 접근할 수 있습니다.</Alert>
        <LinkButton to="/" variant="secondary" block>
          사용자 화면으로
        </LinkButton>
      </div>
    )
  }
  return children
}
