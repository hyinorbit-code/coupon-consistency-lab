import { useEffect, useRef, useState } from 'react'
import { Link, useSearchParams } from 'react-router'
import { verifyEmail } from '../api/auth'

type Result = { kind: 'pending' } | { kind: 'success'; message: string } | { kind: 'failure'; message: string }

/**
 * 메일 링크로 들어오는 페이지. 링크를 여는 것(GET)만으로는 인증되지 않고, 이 페이지가 POST로 토큰을 보낸다.
 * 메일 보안 스캐너가 링크를 미리 열어도 토큰이 소진되지 않게 하기 위해서다(ADR-0012).
 */
export function VerifyEmailPage() {
  const [searchParams] = useSearchParams()
  const token = searchParams.get('token')
  const [result, setResult] = useState<Result>({ kind: 'pending' })
  // 개발 모드의 StrictMode는 effect를 두 번 실행한다. 두 번째 요청은 "이미 사용한 토큰"으로 실패하므로 한 번만 보낸다.
  const requested = useRef(false)

  useEffect(() => {
    if (!token || requested.current) {
      return
    }
    requested.current = true
    verifyEmail(token)
      .then((response) => setResult({ kind: 'success', message: response.message }))
      .catch((e: unknown) =>
        setResult({ kind: 'failure', message: e instanceof Error ? e.message : '인증에 실패했습니다' }),
      )
  }, [token])

  if (!token) {
    return (
      <section>
        <h1>이메일 인증</h1>
        <p className="error">인증 링크가 올바르지 않습니다.</p>
      </section>
    )
  }

  return (
    <section>
      <h1>이메일 인증</h1>
      {result.kind === 'pending' && <p>인증하는 중…</p>}
      {result.kind === 'success' && (
        <>
          <p>{result.message}</p>
          <Link to="/login">로그인하러 가기</Link>
        </>
      )}
      {result.kind === 'failure' && (
        <>
          <p className="error">{result.message}</p>
          <Link to="/signup">회원가입 화면으로</Link>
        </>
      )}
    </section>
  )
}
