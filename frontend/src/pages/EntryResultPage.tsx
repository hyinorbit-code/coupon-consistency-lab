import { useLocation, useParams } from 'react-router'
import type { Prize } from '../api/types'
import { PrizeCard } from '../components/PrizeCard'
import { Card, LinkButton } from '../components/ui'

type ResultState = { outcome: 'won'; prize: Prize } | { outcome: 'sold-out'; title?: string } | null

export function EntryResultPage() {
  const { id } = useParams()
  const state = useLocation().state as ResultState

  if (state?.outcome === 'won') {
    return (
      <div className="space-y-5">
        <div className="pt-4 text-center">
          <p className="text-5xl" aria-hidden>
            🎉
          </p>
          <h1 className="mt-3 text-2xl font-bold">당첨을 축하드려요!</h1>
          <p className="mt-1 text-sm text-stone-500">아래 바코드를 매장에서 보여 주세요.</p>
        </div>
        <PrizeCard prize={state.prize} />
        <LinkButton to="/my/prizes" variant="secondary" block>
          내 당첨 내역 보기
        </LinkButton>
      </div>
    )
  }

  if (state?.outcome === 'sold-out') {
    return (
      <div className="space-y-5 pt-4">
        <div className="text-center">
          <p className="text-5xl" aria-hidden>
            🥲
          </p>
          <h1 className="mt-3 text-2xl font-bold">아쉽지만 마감되었어요</h1>
          <p className="mt-1 text-sm text-stone-500">{state.title ?? '이번 이벤트'}의 경품이 모두 소진되었습니다.</p>
        </div>
        <Card className="text-sm text-stone-600">다음 드롭은 홈에서 확인할 수 있어요. 오픈 시각에 맞춰 미리 로그인해 두면 더 빨리 응모할 수 있어요.</Card>
        <LinkButton to="/" block>
          다른 이벤트 보기
        </LinkButton>
      </div>
    )
  }

  return (
    <div className="space-y-4 pt-4 text-center">
      <p className="text-stone-600">응모 결과를 찾을 수 없어요.</p>
      <LinkButton to={`/drops/${id}`} variant="secondary">
        이벤트로 돌아가기
      </LinkButton>
    </div>
  )
}
