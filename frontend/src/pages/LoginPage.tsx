import { useState, type FormEvent } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router'
import { login } from '../api/auth'
import { ApiError, errorMessage } from '../api/client'
import { useAuth } from '../auth/useAuth'
import { Alert, Button, Card, Field } from '../components/ui'
import { safeNext } from '../lib/navigation'

export function LoginPage() {
  const { signIn } = useAuth()
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const next = safeNext(searchParams.get('next'))
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<ApiError | string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    setSubmitting(true)
    try {
      const { accessToken } = await login(email, password)
      await signIn(accessToken)
      navigate(next, { replace: true })
    } catch (e) {
      setError(e instanceof ApiError ? e : errorMessage(e, '로그인하지 못했습니다.'))
    } finally {
      setSubmitting(false)
    }
  }

  function handleKakao() {
    // 실제로는 카카오 인가 화면으로 이동했다가 인가 코드와 함께 돌아온다. 목업에서는 바로 콜백으로 간다.
    navigate(`/auth/kakao/callback?mode=login&code=mock-authorization-code&next=${encodeURIComponent(next)}`)
  }

  return (
    <div className="mx-auto max-w-sm space-y-6 pt-4">
      <div className="text-center">
        <h1 className="text-2xl font-bold">로그인</h1>
        <p className="mt-1 text-sm text-stone-500">응모하려면 카카오 계정 연동이 필요해요.</p>
      </div>

      <Button variant="kakao" size="lg" block onClick={handleKakao}>
        <span aria-hidden>💬</span> 카카오로 시작하기
      </Button>

      <div className="flex items-center gap-3 text-xs text-stone-400">
        <span className="h-px flex-1 bg-stone-200" />
        또는 이메일로 로그인
        <span className="h-px flex-1 bg-stone-200" />
      </div>

      <Card>
        <form onSubmit={handleSubmit} className="space-y-4">
          <Field label="이메일" name="email" type="email" autoComplete="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
          <Field
            label="비밀번호"
            name="password"
            type="password"
            autoComplete="current-password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
          />
          {error && <LoginError error={error} email={email} />}
          <Button type="submit" block size="lg" loading={submitting}>
            로그인
          </Button>
        </form>
      </Card>

      <p className="text-center text-sm text-stone-500">
        아직 계정이 없나요?{' '}
        <Link to="/signup" className="font-semibold text-brand-700">
          이메일로 가입하기
        </Link>
      </p>
    </div>
  )
}

function LoginError({ error, email }: { error: ApiError | string; email: string }) {
  if (typeof error === 'string') return <Alert kind="error">{error}</Alert>
  if (error.code === 'EMAIL_NOT_VERIFIED') {
    return (
      <Alert kind="warning">
        {error.message}{' '}
        <Link to="/signup/done" state={{ email }} className="font-semibold underline">
          인증 메일 다시 받기
        </Link>
      </Alert>
    )
  }
  return <Alert kind="error">{error.message}</Alert>
}
