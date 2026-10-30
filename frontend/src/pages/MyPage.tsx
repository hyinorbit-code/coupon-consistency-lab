import { Link, useNavigate, useSearchParams } from 'react-router'
import { useAuth } from '../auth/useAuth'
import { Alert, Badge, Button, Card, PageHeader } from '../components/ui'
import { safeNext } from '../lib/navigation'

export function MyPage() {
  const { me, signOut } = useAuth()
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const next = searchParams.get('next')

  if (!me) return null

  function handleLinkKakao() {
    // 실제로는 카카오 인가 화면을 거쳐 돌아온다. 목업에서는 바로 콜백으로 간다.
    const afterLink = safeNext(next, '/my')
    navigate(`/auth/kakao/callback?mode=link&code=mock-authorization-code&next=${encodeURIComponent(afterLink)}`)
  }

  function handleSignOut() {
    signOut()
    navigate('/', { replace: true })
  }

  return (
    <div className="space-y-5">
      <PageHeader title="마이페이지" />

      {next && !me.kakaoLinked && <Alert kind="info">응모하려면 먼저 카카오 계정을 연동해 주세요.</Alert>}

      <Card>
        <div className="flex items-center gap-4">
          <span className="flex size-14 items-center justify-center rounded-full bg-brand-100 text-xl font-bold text-brand-700">
            {me.nickname.slice(0, 1)}
          </span>
          <div className="min-w-0">
            <div className="flex items-center gap-2">
              <p className="truncate text-lg font-semibold">{me.nickname}</p>
              {me.role === 'ADMIN' && <Badge tone="stone">관리자</Badge>}
            </div>
            <p className="truncate text-sm text-stone-500">{me.email ?? '카카오로 가입한 계정'}</p>
          </div>
        </div>
      </Card>

      <Card className="space-y-3">
        <div className="flex items-center justify-between">
          <div>
            <p className="font-semibold">카카오 계정</p>
            <p className="text-sm text-stone-500">응모하려면 카카오 계정 연동이 필요해요. 카카오 계정당 한 번만 응모할 수 있어요.</p>
          </div>
        </div>
        {me.kakaoLinked ? (
          <div className="flex items-center gap-2 rounded-xl bg-[#FEE500]/30 px-4 py-3 text-sm font-medium">
            <span aria-hidden>💬</span> 카카오 계정이 연동되어 있어요
          </div>
        ) : (
          <Button variant="kakao" block size="lg" onClick={handleLinkKakao}>
            카카오 계정 연동하기
          </Button>
        )}
      </Card>

      <Card className="divide-y divide-stone-100 p-0">
        <MenuLink to="/my/prizes" label="내 당첨 내역" />
        <MenuLink to="/terms" label="이용약관" />
        <MenuLink to="/privacy" label="개인정보 처리방침" />
      </Card>

      <div className="grid gap-2">
        <Button variant="secondary" block onClick={handleSignOut}>
          로그아웃
        </Button>
        <Button variant="ghost" block disabled title="회원 탈퇴는 준비 중입니다.">
          회원 탈퇴 (준비 중)
        </Button>
      </div>
    </div>
  )
}

function MenuLink({ to, label }: { to: string; label: string }) {
  return (
    <Link to={to} className="flex h-12 items-center justify-between px-5 text-sm font-medium text-stone-700 hover:bg-stone-50">
      {label}
      <span aria-hidden className="text-stone-400">
        ›
      </span>
    </Link>
  )
}
