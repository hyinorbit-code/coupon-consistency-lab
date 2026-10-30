import { useEffect, useRef } from 'react'
import JsBarcode from 'jsbarcode'
import type { DropPhase, DropStatus } from '../api/types'
import { remainingUntil } from '../lib/time'
import { useNow } from '../lib/useNow'
import { Badge } from './ui'

const PHASE_LABELS: Record<DropPhase, { label: string; tone: 'green' | 'blue' | 'red' | 'stone' }> = {
  OPEN: { label: '진행 중', tone: 'green' },
  UPCOMING: { label: '오픈 예정', tone: 'blue' },
  SOLD_OUT: { label: '선착순 마감', tone: 'red' },
  CLOSED: { label: '종료', tone: 'stone' },
}

export function PhaseBadge({ phase }: { phase: DropPhase }) {
  const { label, tone } = PHASE_LABELS[phase]
  return <Badge tone={tone}>{label}</Badge>
}

const STATUS_LABELS: Record<DropStatus, { label: string; tone: 'amber' | 'brand' | 'stone' }> = {
  DRAFT: { label: '준비 중', tone: 'amber' },
  PUBLISHED: { label: '공개', tone: 'brand' },
  CANCELED: { label: '취소', tone: 'stone' },
}

export function StatusBadge({ status }: { status: DropStatus }) {
  const { label, tone } = STATUS_LABELS[status]
  return <Badge tone={tone}>{label}</Badge>
}

/** 오픈까지 남은 시간. 0이 되면 onReached를 한 번 호출한다. */
export function Countdown({ targetIso, onReached }: { targetIso: string; onReached?: () => void }) {
  const now = useNow(250)
  const target = Date.parse(targetIso)
  const left = remainingUntil(target, now)
  const reached = useRef(false)

  useEffect(() => {
    if (left.totalMs === 0 && !reached.current) {
      reached.current = true
      onReached?.()
    }
  }, [left.totalMs, onReached])

  const pad = (value: number) => String(value).padStart(2, '0')
  return (
    <div className="flex items-end justify-center gap-2 font-mono tabular-nums" aria-label="오픈까지 남은 시간">
      {left.days > 0 && <Unit value={String(left.days)} label="일" />}
      <Unit value={pad(left.hours)} label="시간" />
      <span className="pb-6 text-2xl text-stone-300">:</span>
      <Unit value={pad(left.minutes)} label="분" />
      <span className="pb-6 text-2xl text-stone-300">:</span>
      <Unit value={pad(left.seconds)} label="초" />
    </div>
  )
}

function Unit({ value, label }: { value: string; label: string }) {
  return (
    <div className="flex flex-col items-center">
      <span className="min-w-14 rounded-xl bg-stone-900 px-2 py-2 text-center text-3xl font-bold text-white">{value}</span>
      <span className="mt-1 text-xs text-stone-500">{label}</span>
    </div>
  )
}

export function StockBar({ remaining, total }: { remaining: number; total: number }) {
  const ratio = total === 0 ? 0 : remaining / total
  return (
    <div>
      <div className="mb-1.5 flex items-baseline justify-between text-sm">
        <span className="text-stone-500">남은 수량</span>
        <span className="font-semibold tabular-nums">
          <span className={remaining === 0 ? 'text-red-600' : 'text-brand-600'}>{remaining}</span>
          <span className="text-stone-400"> / {total}</span>
        </span>
      </div>
      <div className="h-2.5 overflow-hidden rounded-full bg-stone-200">
        <div
          className="h-full rounded-full bg-brand-500 transition-[width] duration-500"
          style={{ width: `${Math.round(ratio * 100)}%` }}
        />
      </div>
    </div>
  )
}

/** 경품 코드 바코드(Code 128). */
export function Barcode({ value }: { value: string }) {
  const ref = useRef<SVGSVGElement>(null)
  useEffect(() => {
    if (ref.current) {
      JsBarcode(ref.current, value, { format: 'CODE128', displayValue: false, height: 72, margin: 0, width: 1.6 })
    }
  }, [value])
  return <svg ref={ref} className="mx-auto h-[72px] max-w-full" role="img" aria-label={`바코드 ${value}`} />
}

/** 경품 이미지 자리. 실제 이미지가 생기기 전까지 쓰는 커피 일러스트 카드. */
export function PrizeVisual({ compact = false }: { compact?: boolean }) {
  return (
    <div
      className={`flex items-center justify-center rounded-2xl bg-gradient-to-br from-brand-100 via-amber-50 to-orange-200 ${compact ? 'size-16 text-3xl' : 'h-44 text-7xl'}`}
      aria-hidden
    >
      ☕
    </div>
  )
}
