import { apiRequest } from './client'
import type { DropDetail, DropStock, DropSummary, EntryResponse, Prize } from './types'

export function fetchDrops() {
  return apiRequest<DropSummary[]>('/drops')
}

export function fetchDrop(id: number, token: string | null) {
  return apiRequest<DropDetail>(`/drops/${id}`, { token })
}

export function fetchDropStock(id: number) {
  return apiRequest<DropStock>(`/drops/${id}/stock`)
}

/** 응모. 이미 당첨됐으면 같은 경품을 다시 돌려준다(멱등성). */
export function enterDrop(id: number, token: string) {
  return apiRequest<EntryResponse>(`/drops/${id}/entries`, { method: 'POST', token })
}

export function fetchMyPrizes(token: string) {
  return apiRequest<Prize[]>('/users/me/prizes', { token })
}
