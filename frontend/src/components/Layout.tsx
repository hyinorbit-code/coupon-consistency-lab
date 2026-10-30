import { Link, NavLink, Outlet } from 'react-router'
import { useAuth } from '../auth/useAuth'

function navClass({ isActive }: { isActive: boolean }) {
  return `whitespace-nowrap rounded-lg px-2 py-1.5 text-[13px] font-medium sm:px-2.5 sm:text-sm ${isActive ? 'text-brand-700' : 'text-stone-600 hover:text-stone-900'}`
}

export function Logo() {
  return (
    <Link to="/" className="flex shrink-0 items-center gap-1.5 text-base font-extrabold tracking-tight sm:text-[17px]">
      <span aria-hidden>☕</span>
      <span>
        Coupon<span className="text-brand-600">Lab</span>
      </span>
    </Link>
  )
}

/** 사용자 화면 레이아웃. 모바일 폭을 기준으로 가운데 정렬한다. */
export function Layout() {
  const { me } = useAuth()

  return (
    <div className="flex min-h-dvh flex-col">
      <header className="sticky top-0 z-20 border-b border-stone-200 bg-white/90 backdrop-blur">
        <div className="mx-auto flex h-14 max-w-xl items-center justify-between px-4">
          <Logo />
          <nav className="flex items-center gap-0.5 sm:gap-1">
            <NavLink to="/" end className={navClass}>
              이벤트
            </NavLink>
            {me ? (
              <>
                <NavLink to="/my/prizes" className={navClass}>
                  내 당첨
                </NavLink>
                {me.role === 'ADMIN' && (
                  <NavLink to="/admin" className={navClass}>
                    관리자
                  </NavLink>
                )}
                <NavLink to="/my" end className={navClass}>
                  <span className="inline-flex size-7 items-center justify-center rounded-full bg-brand-100 text-xs font-bold text-brand-700">
                    {me.nickname.slice(0, 1)}
                  </span>
                </NavLink>
              </>
            ) : (
              <NavLink to="/login" className={navClass}>
                로그인
              </NavLink>
            )}
          </nav>
        </div>
      </header>

      <main className="mx-auto w-full max-w-xl flex-1 px-4 py-6">
        <Outlet />
      </main>

      <Footer />
    </div>
  )
}

export function Footer() {
  return (
    <footer className="border-t border-stone-200 bg-white">
      <div className="mx-auto flex max-w-xl flex-col gap-2 px-4 py-6 text-xs text-stone-500">
        <div className="flex gap-4">
          <Link to="/terms" className="hover:text-stone-800">
            이용약관
          </Link>
          <Link to="/privacy" className="font-semibold hover:text-stone-800">
            개인정보 처리방침
          </Link>
        </div>
        <p>Coupon Lab은 개인이 운영하는 비영리 선착순 이벤트 실험 프로젝트입니다.</p>
      </div>
    </footer>
  )
}
