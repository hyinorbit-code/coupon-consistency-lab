import { useCallback, useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router'
import { ApiError, errorMessage } from '../api/client'
import { enterDrop, fetchDrop, fetchDropStock } from '../api/drops'
import type { DropDetail } from '../api/types'
import { useAuth } from '../auth/useAuth'
import { Countdown, PhaseBadge, PrizeVisual, StockBar } from '../components/drop'
import { Alert, Button, Card, LinkButton, LoadingBlock } from '../components/ui'
import { formatKst } from '../lib/time'
import { useNow } from '../lib/useNow'

const STOCK_POLL_MS = 2000
const POLL_BEFORE_OPEN_MS = 5 * 60_000

export function DropDetailPage() {
  const { id } = useParams()
  const dropId = Number(id)
  const navigate = useNavigate()
  const { token, me } = useAuth()
  const [drop, setDrop] = useState<DropDetail | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [entering, setEntering] = useState(false)
  const [entryError, setEntryError] = useState<string | null>(null)

  const load = useCallback(() => {
    fetchDrop(dropId, token)
      .then(setDrop)
      .catch((e: unknown) => setError(errorMessage(e, '이벤트를 불러오지 못했습니다.')))
  }, [dropId, token])

  useEffect(load, [load])

  // 진행 중이거나 곧 열리는 이벤트는 남은 수량을 주기적으로 갱신한다.
  const now = useNow(5000)
  const shouldPoll =
    drop !== null &&
    (drop.phase === 'OPEN' || (drop.phase === 'UPCOMING' && Date.parse(drop.openAt) - now < POLL_BEFORE_OPEN_MS))
  useEffect(() => {
    if (!shouldPoll) return
    const timer = setInterval(() => {
      fetchDropStock(dropId)
        .then((stock) => setDrop((current) => (current ? { ...current, ...stock } : current)))
        .catch(() => undefined)
    }, STOCK_POLL_MS)
    return () => clearInterval(timer)
  }, [dropId, shouldPoll])

  async function handleEnter() {
    if (!token) return
    setEntering(true)
    setEntryError(null)
    try {
      const { prize } = await enterDrop(dropId, token)
      navigate(`/drops/${dropId}/result`, { state: { outcome: 'won', prize } })
    } catch (e) {
      if (e instanceof ApiError && e.code === 'SOLD_OUT') {
        navigate(`/drops/${dropId}/result`, { state: { outcome: 'sold-out', title: drop?.title } })
        return
      }
      if (e instanceof ApiError && (e.code === 'DROP_NOT_OPEN' || e.code === 'DROP_CLOSED')) {
        load()
      }
      setEntryError(errorMessage(e, '응모하지 못했습니다. 다시 시도해 주세요.'))
    } finally {
      setEntering(false)
    }
  }

  if (error) return <Alert kind="error">{error}</Alert>
  if (!drop) return <LoadingBlock />

  return (
    <div className="space-y-5">
      <PrizeVisual />

      <div>
        <PhaseBadge phase={drop.phase} />
        <h1 className="mt-2 text-2xl font-bold tracking-tight">{drop.title}</h1>
        <p className="mt-1 text-stone-600">{drop.prizeDescription}</p>
      </div>

      <Card className="space-y-5">
        {drop.phase === 'UPCOMING' && (
          <div className="space-y-3 text-center">
            <p className="text-sm font-medium text-stone-500">오픈까지</p>
            <Countdown targetIso={drop.openAt} onReached={load} />
          </div>
        )}
        <StockBar remaining={drop.remainingQuantity} total={drop.totalQuantity} />
        <dl className="grid grid-cols-[auto_1fr] gap-x-4 gap-y-1.5 text-sm">
          <dt className="text-stone-500">오픈</dt>
          <dd className="font-medium">{formatKst(drop.openAt)}</dd>
          <dt className="text-stone-500">종료</dt>
          <dd className="font-medium">{formatKst(drop.closeAt)}</dd>
        </dl>
      </Card>

      {entryError && <Alert kind="error">{entryError}</Alert>}

      <div className="sticky bottom-4">
        <EntryAction drop={drop} loggedIn={me !== null} kakaoLinked={me?.kakaoLinked ?? false} entering={entering} onEnter={handleEnter} />
      </div>

      <Card>
        <h2 className="mb-2 font-semibold">참여 안내</h2>
        <ul className="list-disc space-y-1 pl-5 text-sm text-stone-600">
          <li>오픈 시각부터 먼저 응모한 순서대로 당첨됩니다. 서버가 처리한 순서가 기준이에요.</li>
          <li>카카오 계정을 연동한 사용자만, 카카오 계정당 한 번 응모할 수 있어요.</li>
          <li>당첨되면 바로 경품 코드(바코드)를 보여 드리고, 내 당첨 내역에서 다시 볼 수 있어요.</li>
          <li>준비된 수량이 모두 소진되면 종료 시각 전이라도 마감됩니다.</li>
        </ul>
      </Card>
    </div>
  )
}

type EntryActionProps = {
  drop: DropDetail
  loggedIn: boolean
  kakaoLinked: boolean
  entering: boolean
  onEnter: () => void
}

/** 응모 버튼. 로그인·카카오 연동·진행 단계·당첨 여부에 따라 바뀐다. */
function EntryAction({ drop, loggedIn, kakaoLinked, entering, onEnter }: EntryActionProps) {
  const next = encodeURIComponent(`/drops/${drop.id}`)

  if (drop.myPrizeId !== null) {
    return (
      <LinkButton to="/my/prizes" size="lg" block variant="secondary">
        🎉 당첨됐어요! 경품 코드 보기
      </LinkButton>
    )
  }
  if (drop.phase === 'CLOSED') {
    return <Button size="lg" block disabled>응모가 종료되었습니다</Button>
  }
  if (drop.phase === 'SOLD_OUT') {
    return <Button size="lg" block disabled>선착순 마감되었습니다</Button>
  }
  if (!loggedIn) {
    return (
      <LinkButton to={`/login?next=${next}`} size="lg" block>
        로그인하고 응모하기
      </LinkButton>
    )
  }
  if (!kakaoLinked) {
    return (
      <LinkButton to={`/my?next=${next}`} size="lg" block variant="kakao">
        카카오 연동하고 응모하기
      </LinkButton>
    )
  }
  if (drop.phase === 'UPCOMING') {
    return <Button size="lg" block disabled>오픈 후 응모할 수 있어요</Button>
  }
  return (
    <Button size="lg" block loading={entering} onClick={onEnter} className="shadow-lg shadow-brand-600/30">
      {entering ? '응모하는 중…' : '지금 응모하기'}
    </Button>
  )
}
