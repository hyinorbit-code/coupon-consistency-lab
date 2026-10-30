import { getResponse, isPassthroughResponse } from 'msw'
import { persistDb } from './db'
import { handlers } from './handlers'

/**
 * 서비스 워커 없이 MSW 핸들러로 요청을 처리한다. 앱 내장 브라우저처럼 서비스 워커를 쓸 수 없는 환경에서도
 * 목업이 동작하게 하기 위해서다. 핸들러가 없거나 passthrough()를 돌려주면 실제 fetch(Vite 프록시 → 백엔드)로 보낸다.
 * 목업 응답은 브라우저 네트워크 탭에 보이지 않으므로 콘솔에 남긴다.
 */
export async function mockFetch(path: string, init: RequestInit): Promise<Response> {
  const request = new Request(new URL(path, window.location.origin), init)
  const response = await getResponse(handlers, request)
  if (!response || isPassthroughResponse(response)) {
    return fetch(path, init)
  }
  persistDb()
  console.debug(`[mock] ${init.method ?? 'GET'} ${path} → ${response.status}`)
  return response
}
