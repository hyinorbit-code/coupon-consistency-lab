import { useEffect, useState } from 'react'
import { Link } from 'react-router'
import { fetchAdminDrops } from '../../api/admin'
import { errorMessage } from '../../api/client'
import type { AdminDropSummary } from '../../api/types'
import { useAuth } from '../../auth/useAuth'
import { PhaseBadge, StatusBadge } from '../../components/drop'
import { Alert, EmptyState, LinkButton, LoadingBlock, PageHeader } from '../../components/ui'
import { formatKst } from '../../lib/time'

export function AdminDropsPage() {
  const { token } = useAuth()
  const [drops, setDrops] = useState<AdminDropSummary[] | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!token) return
    fetchAdminDrops(token)
      .then(setDrops)
      .catch((e: unknown) => setError(errorMessage(e, '이벤트 목록을 불러오지 못했습니다.')))
  }, [token])

  return (
    <div>
      <PageHeader
        title="이벤트 목록"
        description="준비 중인 이벤트에 코드를 등록하고 공개합니다. 오픈 이후에는 코드를 추가할 수 없습니다."
        action={<LinkButton to="/admin/drops/new">+ 새 이벤트</LinkButton>}
      />
      {error && <Alert kind="error">{error}</Alert>}
      {!drops && !error && <LoadingBlock />}
      {drops?.length === 0 && <EmptyState title="아직 이벤트가 없어요" />}
      {drops && drops.length > 0 && (
        <div className="overflow-x-auto rounded-2xl bg-white shadow-sm ring-1 ring-stone-200">
          <table className="w-full min-w-[720px] text-left text-sm">
            <thead className="border-b border-stone-200 bg-stone-50 text-xs text-stone-500">
              <tr>
                <th className="px-4 py-3 font-medium">이벤트</th>
                <th className="px-4 py-3 font-medium">상태</th>
                <th className="px-4 py-3 font-medium">진행</th>
                <th className="px-4 py-3 font-medium">오픈 (KST)</th>
                <th className="px-4 py-3 text-right font-medium">할당 / 등록</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-stone-100">
              {drops.map((drop) => (
                <tr key={drop.id} className="hover:bg-stone-50">
                  <td className="px-4 py-3">
                    <Link to={`/admin/drops/${drop.id}`} className="font-medium text-stone-900 hover:text-brand-700">
                      {drop.title}
                    </Link>
                    <p className="text-xs text-stone-400">#{drop.id}</p>
                  </td>
                  <td className="px-4 py-3">
                    <StatusBadge status={drop.status} />
                  </td>
                  <td className="px-4 py-3">
                    <PhaseBadge phase={drop.phase} />
                  </td>
                  <td className="px-4 py-3 text-stone-600">{formatKst(drop.openAt)}</td>
                  <td className="px-4 py-3 text-right font-mono tabular-nums">
                    {drop.assignedCodes} / {drop.registeredCodes}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}
