import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router'
import { signUp } from '../api/auth'
import { errorMessage } from '../api/client'
import { Alert, Button, Card, Field } from '../components/ui'

export function SignupPage() {
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [nickname, setNickname] = useState('')
  const [agreed, setAgreed] = useState(false)
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
      setError(errorMessage(e, '가입 요청에 실패했습니다.'))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="mx-auto max-w-sm space-y-6 pt-4">
      <div className="text-center">
        <h1 className="text-2xl font-bold">이메일로 가입하기</h1>
        <p className="mt-1 text-sm text-stone-500">가입 후 이메일 인증을 마치면 로그인할 수 있어요.</p>
      </div>
      <Card>
        <form onSubmit={handleSubmit} className="space-y-4">
          <Field label="이메일" name="email" type="email" autoComplete="email" value={email} onChange={(e) => setEmail(e.target.value)} required maxLength={254} />
          <Field
            label="비밀번호"
            name="password"
            type="password"
            autoComplete="new-password"
            hint="8~64자"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
            minLength={8}
            maxLength={64}
          />
          <Field label="닉네임" name="nickname" hint="당첨자 목록에 표시돼요." value={nickname} onChange={(e) => setNickname(e.target.value)} required maxLength={50} />
          <label className="flex items-start gap-2 text-sm text-stone-600">
            <input type="checkbox" checked={agreed} onChange={(e) => setAgreed(e.target.checked)} className="mt-0.5 size-4 accent-brand-600" required />
            <span>
              <Link to="/terms" className="font-medium underline" target="_blank">
                이용약관
              </Link>
              과{' '}
              <Link to="/privacy" className="font-medium underline" target="_blank">
                개인정보 처리방침
              </Link>
              에 동의합니다.
            </span>
          </label>
          {error && <Alert kind="error">{error}</Alert>}
          <Button type="submit" block size="lg" loading={submitting} disabled={!agreed}>
            가입하기
          </Button>
        </form>
      </Card>
      <p className="text-center text-sm text-stone-500">
        이미 계정이 있나요?{' '}
        <Link to="/login" className="font-semibold text-brand-700">
          로그인
        </Link>
      </p>
    </div>
  )
}
