import { apiRequest } from './client'
import type { AdminDropDetail, AdminDropSummary, CodeUploadResult } from './types'

export type CreateDropRequest = {
  title: string
  prizeDescription: string
  openAt: string
  closeAt: string
}

export function fetchAdminDrops(token: string) {
  return apiRequest<AdminDropSummary[]>('/admin/drops', { token })
}

export function fetchAdminDrop(token: string, id: number) {
  return apiRequest<AdminDropDetail>(`/admin/drops/${id}`, { token })
}

export function createDrop(token: string, request: CreateDropRequest) {
  return apiRequest<AdminDropDetail>('/admin/drops', { method: 'POST', token, body: request })
}

/** 코드 일괄 등록. 오픈 전(DRAFT·PUBLISHED)에만 가능하다(ADR-0006). */
export function uploadCodes(token: string, id: number, codes: string[]) {
  return apiRequest<CodeUploadResult>(`/admin/drops/${id}/codes`, { method: 'POST', token, body: { codes } })
}

export function publishDrop(token: string, id: number) {
  return apiRequest<AdminDropDetail>(`/admin/drops/${id}/publish`, { method: 'POST', token })
}

export function cancelDrop(token: string, id: number) {
  return apiRequest<AdminDropDetail>(`/admin/drops/${id}/cancel`, { method: 'POST', token })
}
