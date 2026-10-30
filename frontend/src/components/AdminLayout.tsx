import { Link, NavLink, Outlet } from 'react-router'
import { useAuth } from '../auth/useAuth'
import { Logo } from './Layout'

function tabClass({ isActive }: { isActive: boolean }) {
  return `border-b-2 px-1 pb-3 text-sm font-medium ${isActive ? 'border-brand-600 text-stone-900' : 'border-transparent text-stone-500 hover:text-stone-800'}`
}

/** 관리자 화면 레이아웃. 표가 있어 사용자 화면보다 넓다. */
export function AdminLayout() {
  const { me } = useAuth()

  return (
    <div className="min-h-dvh">
      <header className="border-b border-stone-200 bg-white">
        <div className="mx-auto max-w-5xl px-4">
          <div className="flex h-14 items-center justify-between">
            <div className="flex items-center gap-3">
              <Logo />
              <span className="rounded-md bg-stone-900 px-2 py-0.5 text-xs font-bold text-white">ADMIN</span>
            </div>
            <div className="flex items-center gap-3 text-sm text-stone-500">
              <span>{me?.nickname}</span>
              <Link to="/" className="font-medium text-stone-700 hover:text-stone-900">
                사용자 화면 →
              </Link>
            </div>
          </div>
          <nav className="flex gap-6">
            <NavLink to="/admin" end className={tabClass}>
              이벤트 목록
            </NavLink>
            <NavLink to="/admin/drops/new" className={tabClass}>
              새 이벤트
            </NavLink>
          </nav>
        </div>
      </header>
      <main className="mx-auto max-w-5xl px-4 py-8">
        <Outlet />
      </main>
    </div>
  )
}
