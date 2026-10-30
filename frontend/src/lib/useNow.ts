import { useEffect, useState } from 'react'

/** intervalMs마다 갱신되는 현재 시각(ms). 카운트다운 표시용이며 응모 가능 여부는 서버가 판단한다. */
export function useNow(intervalMs = 1000): number {
  const [now, setNow] = useState(() => Date.now())
  useEffect(() => {
    const timer = setInterval(() => setNow(Date.now()), intervalMs)
    return () => clearInterval(timer)
  }, [intervalMs])
  return now
}
