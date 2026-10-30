import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router'
import { createDrop } from '../../api/admin'
import { errorMessage } from '../../api/client'
import { useAuth } from '../../auth/useAuth'
import { Alert, Button, Card, Field, PageHeader } from '../../components/ui'
import { kstInputToIso, msToKstInput } from '../../lib/time'

const HOUR = 3_600_000

function tomorrowOnTheHour() {
  const tomorrow = Date.now() + 24 * HOUR
  return tomorrow - (tomorrow % HOUR)
}

export function AdminDropNewPage() {
  const { token } = useAuth()
  const navigate = useNavigate()
  const [title, setTitle] = useState('')
  const [prizeDescription, setPrizeDescription] = useState('편의점 커피 2,000원 교환권')
  // 기본값: 내일 같은 시각(정시)부터 2시간
  const [openAt, setOpenAt] = useState(() => msToKstInput(tomorrowOnTheHour()))
  const [closeAt, setCloseAt] = useState(() => msToKstInput(tomorrowOnTheHour() + 2 * HOUR))
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    if (!token) return
    setError(null)
    setSubmitting(true)
    try {
      const drop = await createDrop(token, {
        title,
        prizeDescription,
        openAt: kstInputToIso(openAt),
        closeAt: kstInputToIso(closeAt),
      })
      navigate(`/admin/drops/${drop.id}`)
    } catch (e) {
      setError(errorMessage(e, '이벤트를 만들지 못했습니다.'))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="max-w-xl">
      <PageHeader title="새 이벤트" description="만든 이벤트는 '준비 중' 상태입니다. 코드를 등록한 뒤 공개하세요." />
      <Card>
        <form onSubmit={handleSubmit} className="space-y-4">
          <Field label="이벤트 제목" value={title} onChange={(e) => setTitle(e.target.value)} placeholder="예: 금요일 저녁 커피 드롭" required maxLength={100} />
          <Field label="경품 설명" value={prizeDescription} onChange={(e) => setPrizeDescription(e.target.value)} required maxLength={500} />
          <div className="grid gap-4 sm:grid-cols-2">
            <Field label="오픈 시각 (KST)" type="datetime-local" value={openAt} onChange={(e) => setOpenAt(e.target.value)} required />
            <Field label="종료 시각 (KST)" type="datetime-local" value={closeAt} onChange={(e) => setCloseAt(e.target.value)} required />
          </div>
          <p className="text-xs text-stone-500">기간은 [오픈, 종료)입니다. 종료 시각 정각부터는 응모할 수 없습니다.</p>
          {error && <Alert kind="error">{error}</Alert>}
          <div className="flex justify-end gap-2">
            <Button type="button" variant="ghost" onClick={() => navigate('/admin')}>
              취소
            </Button>
            <Button type="submit" loading={submitting}>
              만들기
            </Button>
          </div>
        </form>
      </Card>
    </div>
  )
}
