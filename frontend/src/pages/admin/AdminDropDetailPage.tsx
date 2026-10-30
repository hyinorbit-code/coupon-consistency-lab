import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { useParams } from 'react-router'
import { cancelDrop, fetchAdminDrop, publishDrop, uploadCodes } from '../../api/admin'
import { errorMessage } from '../../api/client'
import type { AdminDropDetail } from '../../api/types'
import { useAuth } from '../../auth/useAuth'
import { PhaseBadge, StatusBadge } from '../../components/drop'
import { Alert, Button, Card, EmptyState, LinkButton, LoadingBlock, TextAreaField } from '../../components/ui'
import { formatKst, formatKstFull } from '../../lib/time'

export function AdminDropDetailPage() {
  const { id } = useParams()
  const dropId = Number(id)
  const { token } = useAuth()
  const [drop, setDrop] = useState<AdminDropDetail | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [actionError, setActionError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  const load = useCallback(() => {
    if (!token) return
    fetchAdminDrop(token, dropId)
      .then(setDrop)
      .catch((e: unknown) => setError(errorMessage(e, '이벤트를 불러오지 못했습니다.')))
  }, [token, dropId])

  useEffect(load, [load])

  async function runAction(action: (token: string) => Promise<AdminDropDetail>) {
    if (!token) return
    setActionError(null)
    setBusy(true)
    try {
      setDrop(await action(token))
    } catch (e) {
      setActionError(errorMessage(e, '처리하지 못했습니다.'))
    } finally {
      setBusy(false)
    }
  }

  if (error) return <Alert kind="error">{error}</Alert>
  if (!drop) return <LoadingBlock />

  const remaining = drop.registeredCodes - drop.assignedCodes
  const codesLocked = drop.status === 'CANCELED' || drop.phase !== 'UPCOMING'
  const consistent = drop.assignedCodes <= drop.registeredCodes && drop.winners.length === drop.assignedCodes

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <StatusBadge status={drop.status} />
            <PhaseBadge phase={drop.phase} />
            <span className="text-xs text-stone-400">#{drop.id}</span>
          </div>
          <h1 className="mt-2 text-2xl font-bold">{drop.title}</h1>
          <p className="text-sm text-stone-500">{drop.prizeDescription}</p>
          <p className="mt-1 text-sm text-stone-500">
            {formatKst(drop.openAt)} ~ {formatKst(drop.closeAt)}
          </p>
        </div>
        <div className="flex gap-2">
          {drop.status === 'PUBLISHED' && (
            <LinkButton to={`/drops/${drop.id}`} variant="secondary">
              사용자 화면 보기
            </LinkButton>
          )}
          {drop.status === 'DRAFT' && (
            <Button loading={busy} onClick={() => runAction((t) => publishDrop(t, dropId))}>
              공개하기
            </Button>
          )}
          {drop.status !== 'CANCELED' && drop.phase === 'UPCOMING' && (
            <Button
              variant="danger"
              disabled={busy}
              onClick={() => {
                if (window.confirm('이 이벤트를 취소할까요? 사용자 화면에서 사라집니다.')) {
                  runAction((t) => cancelDrop(t, dropId))
                }
              }}
            >
              이벤트 취소
            </Button>
          )}
        </div>
      </div>

      {actionError && <Alert kind="error">{actionError}</Alert>}

      <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
        <Stat label="등록 코드" value={drop.registeredCodes} />
        <Stat label="할당(당첨)" value={drop.assignedCodes} accent />
        <Stat label="남은 코드" value={remaining} />
        <Stat label="참여 시도" value={drop.entryAttempts} hint="매진 응답 포함, 메트릭" />
      </div>

      <Alert kind={consistent ? 'success' : 'error'}>
        {consistent
          ? `대사 정상: 할당 ${drop.assignedCodes} ≤ 등록 ${drop.registeredCodes}, 당첨자 ${drop.winners.length}명 = 할당 코드 ${drop.assignedCodes}개`
          : '대사 불일치: 할당·당첨자 수를 확인하세요.'}
      </Alert>

      <div className="grid gap-6 lg:grid-cols-[1fr_1.2fr]">
        <CodeUpload dropId={dropId} locked={codesLocked} onUploaded={load} />
        <Winners drop={drop} />
      </div>
    </div>
  )
}

function Stat({ label, value, hint, accent = false }: { label: string; value: number; hint?: string; accent?: boolean }) {
  return (
    <Card className="p-4">
      <p className="text-xs text-stone-500">{label}</p>
      <p className={`mt-1 text-2xl font-bold tabular-nums ${accent ? 'text-brand-600' : ''}`}>{value.toLocaleString()}</p>
      {hint && <p className="mt-0.5 text-[11px] text-stone-400">{hint}</p>}
    </Card>
  )
}

function CodeUpload({ dropId, locked, onUploaded }: { dropId: number; locked: boolean; onUploaded: () => void }) {
  const { token } = useAuth()
  const [text, setText] = useState('')
  const [result, setResult] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const codes = text.split('\n').map((line) => line.trim()).filter(Boolean)

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    if (!token) return
    setError(null)
    setResult(null)
    setSubmitting(true)
    try {
      const uploaded = await uploadCodes(token, dropId, codes)
      setResult(`${uploaded.registered}개 등록${uploaded.duplicates ? `, 중복 ${uploaded.duplicates}개 제외` : ''}`)
      setText('')
      onUploaded()
    } catch (e) {
      setError(errorMessage(e, '코드를 등록하지 못했습니다.'))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <Card>
      <h2 className="font-semibold">경품 코드 등록</h2>
      <p className="mt-1 text-xs text-stone-500">
        한 줄에 하나씩 붙여 넣으세요. 코드는 암호화해 저장하고 중복은 자동으로 제외합니다. 오픈 이후에는 수량을 바꿀 수 없습니다.
      </p>
      {locked ? (
        <div className="mt-4">
          <Alert kind="info">오픈 이후이거나 취소된 이벤트라 코드를 등록할 수 없습니다.</Alert>
        </div>
      ) : (
        <form onSubmit={handleSubmit} className="mt-4 space-y-3">
          <TextAreaField
            label={`코드 (${codes.length}개)`}
            value={text}
            onChange={(e) => setText(e.target.value)}
            placeholder={'CPN-AAAA-BBBB-CCCC\nCPN-DDDD-EEEE-FFFF'}
            rows={8}
          />
          {result && <Alert kind="success">{result}</Alert>}
          {error && <Alert kind="error">{error}</Alert>}
          <Button type="submit" block loading={submitting} disabled={codes.length === 0}>
            {codes.length}개 등록하기
          </Button>
        </form>
      )}
    </Card>
  )
}

function Winners({ drop }: { drop: AdminDropDetail }) {
  return (
    <Card className="p-0">
      <div className="flex items-center justify-between px-5 py-4">
        <h2 className="font-semibold">당첨자</h2>
        <span className="text-xs text-stone-400">코드는 끝 4자리만 표시</span>
      </div>
      {drop.winners.length === 0 ? (
        <div className="px-5 pb-5">
          <EmptyState title="아직 당첨자가 없어요" />
        </div>
      ) : (
        <div className="max-h-96 overflow-y-auto">
          <table className="w-full text-left text-sm">
            <thead className="sticky top-0 bg-stone-50 text-xs text-stone-500">
              <tr>
                <th className="px-5 py-2 font-medium">#</th>
                <th className="px-5 py-2 font-medium">닉네임</th>
                <th className="px-5 py-2 font-medium">코드</th>
                <th className="px-5 py-2 font-medium">당첨 시각 (KST)</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-stone-100">
              {drop.winners.map((winner, index) => (
                <tr key={`${winner.codeHint}-${winner.assignedAt}`}>
                  <td className="px-5 py-2 text-stone-400 tabular-nums">{drop.winners.length - index}</td>
                  <td className="px-5 py-2">{winner.nickname}</td>
                  <td className="px-5 py-2 font-mono text-stone-500">****-{winner.codeHint}</td>
                  <td className="px-5 py-2 font-mono text-xs text-stone-500">{formatKstFull(winner.assignedAt)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </Card>
  )
}
