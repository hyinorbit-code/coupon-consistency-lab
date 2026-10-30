import { useEffect, useState } from 'react'
import { Link } from 'react-router'
import { errorMessage } from '../api/client'
import { fetchDrops } from '../api/drops'
import type { DropSummary } from '../api/types'
import { PhaseBadge, PrizeVisual } from '../components/drop'
import { Alert, EmptyState, LoadingBlock } from '../components/ui'
import { formatKst } from '../lib/time'

export function HomePage() {
  const [drops, setDrops] = useState<DropSummary[] | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    fetchDrops()
      .then(setDrops)
      .catch((e: unknown) => setError(errorMessage(e, '이벤트를 불러오지 못했습니다.')))
  }, [])

  const live = drops?.filter((drop) => drop.phase === 'OPEN') ?? []
  const upcoming = (drops?.filter((drop) => drop.phase === 'UPCOMING') ?? []).sort((a, b) => a.openAt.localeCompare(b.openAt))
  const ended = drops?.filter((drop) => drop.phase === 'SOLD_OUT' || drop.phase === 'CLOSED') ?? []

  return (
    <div className="space-y-8">
      <section className="rounded-3xl bg-stone-900 px-6 py-7 text-white">
        <p className="text-sm font-semibold text-brand-500">선착순 무료 드롭</p>
        <h1 className="mt-1 text-2xl leading-snug font-bold">
          정해진 시각, 정해진 수량.
          <br />
          가장 빠른 사람에게 커피 한 잔.
        </h1>
        <p className="mt-3 text-sm text-stone-400">카카오 계정당 한 번만 응모할 수 있어요.</p>
      </section>

      {error && <Alert kind="error">{error}</Alert>}
      {!drops && !error && <LoadingBlock />}

      {drops && (
        <>
          <DropSection title="지금 진행 중" drops={live} empty="진행 중인 이벤트가 없어요." />
          <DropSection title="오픈 예정" drops={upcoming} empty="예정된 이벤트가 없어요." />
          {ended.length > 0 && <DropSection title="종료된 이벤트" drops={ended} muted />}
        </>
      )}
    </div>
  )
}

function DropSection({ title, drops, empty, muted = false }: { title: string; drops: DropSummary[]; empty?: string; muted?: boolean }) {
  return (
    <section>
      <h2 className="mb-3 text-lg font-bold">{title}</h2>
      {drops.length === 0 && empty ? (
        <EmptyState title={empty} />
      ) : (
        <ul className="space-y-3">
          {drops.map((drop) => (
            <li key={drop.id}>
              <DropCard drop={drop} muted={muted} />
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}

function DropCard({ drop, muted }: { drop: DropSummary; muted: boolean }) {
  return (
    <Link
      to={`/drops/${drop.id}`}
      className={`flex gap-4 rounded-2xl bg-white p-4 shadow-sm ring-1 ring-stone-200 transition hover:ring-brand-300 ${muted ? 'opacity-70' : ''}`}
    >
      <PrizeVisual compact />
      <div className="min-w-0 flex-1">
        <div className="flex items-center gap-2">
          <PhaseBadge phase={drop.phase} />
          {drop.phase === 'OPEN' && (
            <span className="text-xs font-semibold text-brand-600">{drop.remainingQuantity}개 남음</span>
          )}
        </div>
        <p className="mt-1 truncate font-semibold">{drop.title}</p>
        <p className="truncate text-sm text-stone-500">{drop.prizeDescription}</p>
        <p className="mt-1 text-xs text-stone-400">
          {drop.phase === 'UPCOMING' ? `${formatKst(drop.openAt)} 오픈` : `${formatKst(drop.openAt)} 오픈 · ${drop.totalQuantity}명`}
        </p>
      </div>
    </Link>
  )
}
