import { useState } from 'react'
import { useLocation } from 'react-router'
import { resendVerificationEmail } from '../api/auth'
import { errorMessage } from '../api/client'
import { Alert, Button, Card, Field, LinkButton } from '../components/ui'

export function SignupDonePage() {
  const location = useLocation()
  const [email, setEmail] = useState((location.state as { email?: string } | null)?.email ?? '')
  const [message, setMessage] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [sending, setSending] = useState(false)

  async function handleResend() {
    setMessage(null)
    setError(null)
    setSending(true)
    try {
      setMessage((await resendVerificationEmail(email)).message)
    } catch (e) {
      setError(errorMessage(e, '재발송 요청에 실패했습니다.'))
    } finally {
      setSending(false)
    }
  }

  return (
    <div className="mx-auto max-w-sm space-y-6 pt-4 text-center">
      <p className="text-5xl" aria-hidden>
        📮
      </p>
      <div>
        <h1 className="text-2xl font-bold">메일을 확인해 주세요</h1>
        <p className="mt-2 text-sm text-stone-600">
          {email ? <strong>{email}</strong> : '입력한 이메일'}로 안내 메일을 보냈어요.
          <br />
          메일의 링크를 눌러 인증을 완료해 주세요.
        </p>
      </div>
      <Card className="space-y-3 text-left">
        <p className="text-sm font-medium">메일이 오지 않았나요?</p>
        <p className="text-xs text-stone-500">스팸함도 확인해 주세요. 인증 메일은 1분에 한 번 다시 받을 수 있어요.</p>
        <Field label="이메일" type="email" value={email} onChange={(e) => setEmail(e.target.value)} />
        <Button variant="secondary" block onClick={handleResend} loading={sending} disabled={!email}>
          인증 메일 다시 보내기
        </Button>
        {message && <Alert kind="success">{message}</Alert>}
        {error && <Alert kind="error">{error}</Alert>}
      </Card>
      <LinkButton to="/login" variant="ghost">
        로그인 화면으로
      </LinkButton>
    </div>
  )
}
