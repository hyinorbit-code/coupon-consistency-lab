import { mocksEnabled } from '../mocks/enabled'
import type { ErrorCode } from './types'

export class ApiError extends Error {
  readonly status: number
  readonly code: ErrorCode | null

  constructor(status: number, message: string, code: ErrorCode | null) {
    super(message)
    this.status = status
    this.code = code
  }
}

type RequestOptions = {
  method?: 'GET' | 'POST' | 'PATCH' | 'DELETE'
  body?: unknown
  token?: string | null
}

/**
 * 백엔드 API 호출. 경로는 /api 아래 상대 경로이며, 로컬에서는 Vite 프록시를 거친다.
 * 오류 응답은 ProblemDetail(RFC 9457)의 detail을 메시지로, code 속성을 업무 오류 코드로 쓴다.
 */
export async function apiRequest<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const headers: Record<string, string> = {}
  if (options.body !== undefined) {
    headers['Content-Type'] = 'application/json'
  }
  if (options.token) {
    headers.Authorization = `Bearer ${options.token}`
  }

  const response = await send(`/api${path}`, {
    method: options.method ?? 'GET',
    headers,
    body: options.body === undefined ? undefined : JSON.stringify(options.body),
  })

  if (!response.ok) {
    throw await toApiError(response)
  }
  if (response.status === 204) {
    return undefined as T
  }
  return (await response.json()) as T
}

let mockFetchModule: Promise<typeof import('../mocks/mockFetch')> | null = null

/** 개발 목업 모드면 MSW 핸들러를 먼저 거친다. 운영 빌드에서는 이 분기가 통째로 빠진다. */
function send(path: string, init: RequestInit): Promise<Response> {
  if (import.meta.env.DEV && mocksEnabled) {
    mockFetchModule ??= import('../mocks/mockFetch')
    return mockFetchModule.then(({ mockFetch }) => mockFetch(path, init))
  }
  return fetch(path, init)
}

async function toApiError(response: Response): Promise<ApiError> {
  try {
    const problem = (await response.json()) as { detail?: string; title?: string; code?: ErrorCode }
    return new ApiError(
      response.status,
      problem.detail ?? problem.title ?? defaultMessage(response.status),
      problem.code ?? null,
    )
  } catch {
    return new ApiError(response.status, defaultMessage(response.status), null)
  }
}

function defaultMessage(status: number): string {
  if (status === 401) return '로그인이 필요합니다.'
  if (status === 403) return '권한이 없습니다.'
  if (status >= 500) return '서버에 문제가 생겼습니다. 잠시 후 다시 시도해 주세요.'
  return `요청을 처리하지 못했습니다. (${status})`
}

export function errorMessage(error: unknown, fallback: string): string {
  return error instanceof Error ? error.message : fallback
}
