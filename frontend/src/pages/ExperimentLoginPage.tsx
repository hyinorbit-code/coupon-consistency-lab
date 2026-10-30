import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router'
import { issueExperimentToken, type Role } from '../api/auth'
import { useAuth } from '../auth/useAuth'

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
      signIn(accessToken)
      navigate('/me')
    } catch (e) {
      setError(e instanceof Error ? e.message : '토큰 발급에 실패했습니다')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <section>
      <h1>실험용 로그인</h1>
      <p className="hint">experiment 프로필에서만 동작합니다. 같은 식별자로 다시 로그인하면 같은 사용자입니다.</p>
      <form onSubmit={handleSubmit}>
        <label>
          식별자
          <input value={providerUserId} onChange={(e) => setProviderUserId(e.target.value)} required maxLength={64} />
        </label>
        <label>
          닉네임
          <input value={nickname} onChange={(e) => setNickname(e.target.value)} required maxLength={50} />
        </label>
        <label>
          역할
          <select value={role} onChange={(e) => setRole(e.target.value as Role)}>
            <option value="USER">USER</option>
            <option value="ADMIN">ADMIN</option>
          </select>
        </label>
        <button type="submit" disabled={submitting}>
          {submitting ? '발급 중…' : '토큰 발급'}
        </button>
      </form>
      {error && <p className="error">{error}</p>}
    </section>
  )
}
