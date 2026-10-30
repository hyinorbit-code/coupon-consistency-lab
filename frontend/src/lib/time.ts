const KST = 'Asia/Seoul'

/** ISO 시각을 한국 시간으로 표시한다. 예: 11월 2일 (월) 오후 8:00 */
export function formatKst(iso: string): string {
  return new Intl.DateTimeFormat('ko-KR', {
    timeZone: KST,
    month: 'long',
    day: 'numeric',
    weekday: 'short',
    hour: 'numeric',
    minute: '2-digit',
  }).format(new Date(iso))
}

export function formatKstFull(iso: string): string {
  return new Intl.DateTimeFormat('ko-KR', {
    timeZone: KST,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false,
  }).format(new Date(iso))
}

export type Remaining = { days: number; hours: number; minutes: number; seconds: number; totalMs: number }

export function remainingUntil(targetMs: number, nowMs: number): Remaining {
  const totalMs = Math.max(0, targetMs - nowMs)
  const totalSeconds = Math.floor(totalMs / 1000)
  return {
    days: Math.floor(totalSeconds / 86_400),
    hours: Math.floor((totalSeconds % 86_400) / 3600),
    minutes: Math.floor((totalSeconds % 3600) / 60),
    seconds: totalSeconds % 60,
    totalMs,
  }
}

/** datetime-local 입력값(YYYY-MM-DDTHH:mm)을 한국 시간으로 해석해 ISO로 바꾼다. */
export function kstInputToIso(value: string): string {
  return new Date(`${value}:00+09:00`).toISOString()
}

/** epoch ms를 한국 시간 기준 datetime-local 입력값으로 바꾼다. */
export function msToKstInput(ms: number): string {
  return new Date(ms + 9 * 3_600_000).toISOString().slice(0, 16)
}
