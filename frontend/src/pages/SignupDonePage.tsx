import { useState } from 'react'
import { useLocation } from 'react-router'
import { resendVerificationEmail } from '../api/auth'

export function SignupDonePage() {
  const location = useLocation()
  const email = (location.state as { email?: string } | null)?.email ?? ''
  const [message, setMessage] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)

  async function handleResend() {
    setMessage(null)
    setError(null)
    try {
      const response = await resendVerificationEmail(email)
      setMessage(response.message)
    } catch (e) {
      setError(e instanceof Error ? e.message : '재발송 요청에 실패했습니다')
    }
  }

  return (
    <section>
      <h1>메일을 확인해 주세요</h1>
      <p>
        {email ? <strong>{email}</strong> : '입력한 이메일'}로 안내 메일을 보냈습니다. 메일의 링크를 눌러 인증을 완료해
        주세요.
      </p>
      <p className="hint">메일이 오지 않았다면 1분 뒤에 다시 요청할 수 있습니다.</p>
      {email && (
        <button type="button" onClick={handleResend}>
          인증 메일 다시 보내기
        </button>
      )}
      {message && <p>{message}</p>}
      {error && <p className="error">{error}</p>}
    </section>
  )
}
