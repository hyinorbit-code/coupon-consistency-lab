import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router'
import { signUp } from '../api/auth'

export function SignupPage() {
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [nickname, setNickname] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    setSubmitting(true)
    try {
      await signUp(email, password, nickname)
      navigate('/signup/done', { state: { email } })
    } catch (e) {
      setError(e instanceof Error ? e.message : '가입 요청에 실패했습니다')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <section>
      <h1>회원가입</h1>
      <form onSubmit={handleSubmit}>
        <label>
          이메일
          <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required maxLength={254} />
        </label>
        <label>
          비밀번호 (8~64자)
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
            minLength={8}
            maxLength={64}
            autoComplete="new-password"
          />
        </label>
        <label>
          닉네임
          <input value={nickname} onChange={(e) => setNickname(e.target.value)} required maxLength={50} />
        </label>
        <button type="submit" disabled={submitting}>
          {submitting ? '요청 중…' : '가입하기'}
        </button>
      </form>
      {error && <p className="error">{error}</p>}
    </section>
  )
}
