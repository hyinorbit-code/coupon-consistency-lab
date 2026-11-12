import { useEffect, useRef, useState } from 'react'
import { useSearchParams } from 'react-router'
import { verifyEmail } from '../api/auth'
import { errorMessage } from '../api/client'
import { Alert, LinkButton, LoadingBlock } from '../components/ui'

type Result = { kind: 'pending' } | { kind: 'success'; message: string } | { kind: 'failure'; message: string }

/**
 * 메일 링크로 들어오는 페이지. 링크를 여는 것(GET)만으로는 인증되지 않고, 이 페이지가 POST로 토큰을 보낸다.
 * 메일 보안 스캐너가 링크를 미리 열어도 토큰이 소진되지 않게 하기 위해서다.
 */
export function VerifyEmailPage() {
  const [searchParams] = useSearchParams()
  const token = searchParams.get('token')
  const [result, setResult] = useState<Result>({ kind: 'pending' })
  // 개발 모드의 StrictMode는 effect를 두 번 실행한다. 두 번째 요청은 "이미 사용한 토큰"으로 실패하므로 한 번만 보낸다.
  const requested = useRef(false)

  useEffect(() => {
    if (!token || requested.current) return
    requested.current = true
    verifyEmail(token)
      .then((response) => setResult({ kind: 'success', message: response.message }))
      .catch((e: unknown) => setResult({ kind: 'failure', message: errorMessage(e, '인증에 실패했습니다.') }))
  }, [token])

  return (
    <div className="mx-auto max-w-sm space-y-6 pt-4 text-center">
      <h1 className="text-2xl font-bold">이메일 인증</h1>
      {!token && <Alert kind="error">인증 링크가 올바르지 않습니다.</Alert>}
      {token && result.kind === 'pending' && <LoadingBlock />}
      {result.kind === 'success' && (
        <>
          <p className="text-5xl" aria-hidden>
            ✅
          </p>
          <Alert kind="success">{result.message}</Alert>
          <LinkButton to="/login" block size="lg">
            로그인하러 가기
          </LinkButton>
        </>
      )}
      {result.kind === 'failure' && (
        <>
          <Alert kind="error">{result.message}</Alert>
          <LinkButton to="/signup/done" variant="secondary" block>
            인증 메일 다시 받기
          </LinkButton>
        </>
      )}
    </div>
  )
}
