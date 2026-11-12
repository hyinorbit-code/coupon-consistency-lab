import { useState } from 'react'
import type { Prize } from '../api/types'
import { formatKst } from '../lib/time'
import { Barcode } from './drop'
import { Button } from './ui'

/**
 * 경품 코드 카드. 바코드와 코드 원문을 보여 주고 카카오톡으로 공유할 수 있다.
 * 공유는 사용자가 직접 보내는 카카오톡 공유 SDK를 쓴다. 지금은 목업이다.
 */
export function PrizeCard({ prize }: { prize: Prize }) {
  const [notice, setNotice] = useState<string | null>(null)

  async function handleCopy() {
    try {
      await navigator.clipboard.writeText(prize.code)
      setNotice('코드를 복사했어요.')
    } catch {
      setNotice('복사하지 못했어요. 코드를 길게 눌러 복사해 주세요.')
    }
  }

  function handleShare() {
    setNotice('카카오톡 공유는 카카오 SDK 연동 후 동작합니다. (목업)')
  }

  return (
    <div className="overflow-hidden rounded-2xl bg-white shadow-sm ring-1 ring-stone-200">
      <div className="bg-stone-900 px-5 py-4 text-white">
        <p className="text-xs text-stone-400">{prize.dropTitle}</p>
        <p className="mt-0.5 font-semibold">{prize.prizeDescription}</p>
      </div>
      <div className="space-y-3 px-5 py-6 text-center">
        <Barcode value={prize.code} />
        <p className="font-mono text-lg font-semibold tracking-wider">{prize.code}</p>
        <p className="text-xs text-stone-400">당첨 시각 {formatKst(prize.wonAt)}</p>
      </div>
      <div className="grid grid-cols-2 gap-2 border-t border-dashed border-stone-200 p-4">
        <Button variant="secondary" onClick={handleCopy}>
          코드 복사
        </Button>
        <Button variant="kakao" onClick={handleShare}>
          카카오톡으로 보내기
        </Button>
      </div>
      {notice && <p className="px-5 pb-4 text-center text-xs text-stone-500">{notice}</p>}
    </div>
  )
}
