import { useState } from 'react'
import { Link, useNavigate } from 'react-router'
import { login } from '../api/auth'
import { apiRequest, errorMessage } from '../api/client'
import { useAuth } from '../auth/useAuth'
import { MOCK_ACCOUNTS, MOCK_PASSWORD } from '../mocks/db'
import { readScenario, writeScenario, type EntryOutcome, type Scenario } from '../mocks/scenario'

const PERSONAS = [
  { label: '일반 사용자', email: MOCK_ACCOUNTS.user },
  { label: '카카오 미연동', email: MOCK_ACCOUNTS.noKakao },
  { label: '관리자', email: MOCK_ACCOUNTS.admin },
] as const

/**
 * 목업 모드 전용 개발 패널. 사용자 전환, 응모 결과 강제, 참여자 시뮬레이션, 목 데이터 초기화를 할 수 있다.
 * 운영 빌드에는 포함되지 않는다.
 */
export function DevPanel() {
  const [open, setOpen] = useState(false)
  const [scenario, setScenario] = useState<Scenario>(readScenario)
  const [message, setMessage] = useState<string | null>(null)
  const { me, signIn, signOut } = useAuth()
  const navigate = useNavigate()

  function update(patch: Partial<Scenario>) {
    const next = { ...scenario, ...patch }
    setScenario(next)
    writeScenario(next)
  }

  async function switchTo(email: string) {
    setMessage(null)
    try {
      const { accessToken } = await login(email, MOCK_PASSWORD)
      await signIn(accessToken)
    } catch (e) {
      setMessage(errorMessage(e, '전환하지 못했습니다.'))
    }
  }

  async function resetData() {
    await apiRequest<void>('/__mock/reset', { method: 'POST' })
    signOut()
    navigate('/')
    window.location.reload()
  }

  if (!open) {
    return (
      <button
        type="button"
        onClick={() => setOpen(true)}
        className="fixed right-4 bottom-4 z-50 rounded-full bg-stone-900 px-3.5 py-2 text-xs font-bold text-white shadow-lg hover:bg-stone-700"
      >
        DEV
      </button>
    )
  }

  return (
    <div className="fixed right-4 bottom-4 z-50 w-72 space-y-4 rounded-2xl bg-stone-900 p-4 text-xs text-stone-200 shadow-2xl">
      <div className="flex items-center justify-between">
        <p className="font-bold text-white">개발 패널 · 목업 모드</p>
        <button type="button" onClick={() => setOpen(false)} className="text-stone-400 hover:text-white" aria-label="닫기">
          ✕
        </button>
      </div>

      <section className="space-y-2">
        <p className="text-stone-400">
          현재: <span className="font-semibold text-white">{me ? `${me.nickname} (${me.role}${me.kakaoLinked ? ', 카카오' : ''})` : '로그아웃'}</span>
        </p>
        <div className="grid grid-cols-2 gap-1.5">
          {PERSONAS.map((persona) => (
            <button key={persona.email} type="button" onClick={() => switchTo(persona.email)} className="rounded-lg bg-stone-800 px-2 py-1.5 hover:bg-stone-700">
              {persona.label}
            </button>
          ))}
          <button type="button" onClick={signOut} className="rounded-lg bg-stone-800 px-2 py-1.5 hover:bg-stone-700">
            로그아웃
          </button>
        </div>
        <Link to="/experiment-login" className="block text-stone-400 underline hover:text-white">
          실제 백엔드 실험용 로그인 →
        </Link>
      </section>

      <section className="space-y-2">
        <label className="grid gap-1">
          <span className="text-stone-400">다음 응모 결과</span>
          <select
            value={scenario.entryOutcome}
            onChange={(e) => update({ entryOutcome: e.target.value as EntryOutcome })}
            className="rounded-lg bg-stone-800 px-2 py-1.5 text-white"
          >
            <option value="auto">실제 남은 수량대로</option>
            <option value="sold-out">강제 매진</option>
            <option value="server-error">서버 오류(500)</option>
          </select>
        </label>
        <label className="flex items-center gap-2">
          <input type="checkbox" checked={scenario.simulateCrowd} onChange={(e) => update({ simulateCrowd: e.target.checked })} className="accent-brand-500" />
          다른 참여자 시뮬레이션 (수량이 줄어듦)
        </label>
        <label className="grid gap-1">
          <span className="text-stone-400">응답 지연: {scenario.latencyMs}ms</span>
          <input type="range" min={0} max={2000} step={100} value={scenario.latencyMs} onChange={(e) => update({ latencyMs: Number(e.target.value) })} className="accent-brand-500" />
        </label>
      </section>

      <section className="space-y-1.5 border-t border-stone-700 pt-3">
        <button type="button" onClick={resetData} className="w-full rounded-lg bg-red-500/20 px-2 py-1.5 font-semibold text-red-300 hover:bg-red-500/30">
          목 데이터 초기화
        </button>
        <p className="text-[11px] leading-relaxed text-stone-500">
          목업 계정 비밀번호: {MOCK_PASSWORD} · 인증 대기 계정: {MOCK_ACCOUNTS.pending} · 같은 계정으로 5번 틀리면 잠깁니다.
        </p>
        {message && <p className="text-red-300">{message}</p>}
      </section>
    </div>
  )
}
