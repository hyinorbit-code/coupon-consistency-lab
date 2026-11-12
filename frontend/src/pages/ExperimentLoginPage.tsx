import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router'
import { issueExperimentToken } from '../api/auth'
import { errorMessage } from '../api/client'
import type { Role } from '../api/types'
import { useAuth } from '../auth/useAuth'
import { Alert, Button, Card, Field } from '../components/ui'

/**
 * 실제 백엔드의 실험용 토큰 발급(experiment 프로필)으로 로그인한다. 개발 패널에서만 들어온다.
 * 실험용 계정은 응모 자격을 인정받는다.
 */
export function ExperimentLoginPage() {
  const { signIn } = useAuth()
  const navigate = useNavigate()
  const [providerUserId, setProviderUserId] = useState('user-1')
  const [nickname, setNickname] = useState('tester')
  const [role, setRole] = useState<Role>('USER')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    setSubmitting(true)
    try {
      const { accessToken } = await issueExperimentToken(providerUserId, nickname, role)
      await signIn(accessToken)
      navigate('/my')
    } catch (e) {
      setError(errorMessage(e, '토큰 발급에 실패했습니다. 백엔드(bootTestRun)가 실행 중인지 확인해 주세요.'))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="mx-auto max-w-sm space-y-6 pt-4">
      <div>
        <h1 className="text-2xl font-bold">실험용 로그인</h1>
        <p className="mt-1 text-sm text-stone-500">
          실제 백엔드의 실험용 토큰으로 로그인합니다. experiment 프로필에서만 동작하며, 같은 식별자는 같은 사용자입니다.
        </p>
      </div>
      <Card>
        <form onSubmit={handleSubmit} className="space-y-4">
          <Field label="식별자" value={providerUserId} onChange={(e) => setProviderUserId(e.target.value)} required maxLength={64} />
          <Field label="닉네임" value={nickname} onChange={(e) => setNickname(e.target.value)} required maxLength={50} />
          <label className="grid gap-1.5">
            <span className="text-sm font-medium text-stone-700">역할</span>
            <select
              value={role}
              onChange={(e) => setRole(e.target.value as Role)}
              className="h-11 rounded-xl bg-white px-3 ring-1 ring-stone-300 focus:ring-2 focus:ring-brand-500 focus:outline-none"
            >
              <option value="USER">USER</option>
              <option value="ADMIN">ADMIN</option>
            </select>
          </label>
          {error && <Alert kind="error">{error}</Alert>}
          <Button type="submit" block loading={submitting}>
            토큰 발급
          </Button>
        </form>
      </Card>
    </div>
  )
}
