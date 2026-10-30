import { useEffect, useState } from 'react'
import { errorMessage } from '../api/client'
import { fetchMyPrizes } from '../api/drops'
import type { Prize } from '../api/types'
import { useAuth } from '../auth/useAuth'
import { PrizeCard } from '../components/PrizeCard'
import { Alert, EmptyState, LinkButton, LoadingBlock, PageHeader } from '../components/ui'

export function MyPrizesPage() {
  const { token } = useAuth()
  const [prizes, setPrizes] = useState<Prize[] | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!token) return
    fetchMyPrizes(token)
      .then(setPrizes)
      .catch((e: unknown) => setError(errorMessage(e, '당첨 내역을 불러오지 못했습니다.')))
  }, [token])

  return (
    <div>
      <PageHeader title="내 당첨 내역" description="매장에서 바코드를 보여 주면 교환할 수 있어요." />
      {error && <Alert kind="error">{error}</Alert>}
      {!prizes && !error && <LoadingBlock />}
      {prizes?.length === 0 && (
        <EmptyState
          title="아직 당첨 내역이 없어요"
          description="다음 드롭에 도전해 보세요."
          action={<LinkButton to="/">이벤트 보러 가기</LinkButton>}
        />
      )}
      <div className="space-y-4">
        {prizes?.map((prize) => (
          <PrizeCard key={prize.id} prize={prize} />
        ))}
      </div>
    </div>
  )
}
