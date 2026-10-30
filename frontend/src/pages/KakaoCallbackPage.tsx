import { useEffect, useRef, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router'
import { linkKakao, loginWithKakao } from '../api/auth'
import { errorMessage } from '../api/client'
import { useAuth } from '../auth/useAuth'
import { Alert, LinkButton, LoadingBlock } from '../components/ui'
import { safeNext } from '../lib/navigation'

/**
 * 카카오 인가 후 돌아오는 화면. mode=login이면 카카오로 로그인(없으면 가입)하고,
 * mode=link면 로그인한 사용자에 카카오 계정을 연동한다(ADR-0010). 지금은 목업 API를 호출한다.
 */
export function KakaoCallbackPage() {
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()
  const { token, signIn, setMe } = useAuth()
  const [error, setError] = useState<string | null>(null)
  const handled = useRef(false)

  const code = searchParams.get('code')
  const mode = searchParams.get('mode') === 'link' ? 'link' : 'login'
  const next = safeNext(searchParams.get('next'), mode === 'link' ? '/my' : '/')

  useEffect(() => {
    // StrictMode에서 effect가 두 번 실행돼도 인가 코드는 한 번만 쓴다.
    if (handled.current || !code) return
    handled.current = true

    const run = async () => {
      if (mode === 'link') {
        if (!token) throw new Error('로그인한 뒤 카카오 계정을 연동할 수 있습니다.')
        setMe(await linkKakao(token, code))
      } else {
        const { accessToken } = await loginWithKakao(code)
        await signIn(accessToken)
      }
      navigate(next, { replace: true })
    }
    run().catch((e: unknown) => setError(errorMessage(e, '카카오 처리 중 문제가 생겼습니다.')))
  }, [code, mode, next, navigate, signIn, setMe, token])

  if (!code) return <Alert kind="error">카카오 인가 코드가 없습니다.</Alert>
  if (error) {
    return (
      <div className="space-y-4 pt-4">
        <Alert kind="error">{error}</Alert>
        <LinkButton to={mode === 'link' ? '/my' : '/login'} variant="secondary" block>
          돌아가기
        </LinkButton>
      </div>
    )
  }
  return (
    <div className="pt-8 text-center text-sm text-stone-500">
      <LoadingBlock />
      카카오 계정을 확인하고 있어요…
    </div>
  )
}
