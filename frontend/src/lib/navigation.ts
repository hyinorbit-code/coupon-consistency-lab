/** 로그인 후 돌아갈 경로. 외부 주소로의 이동(오픈 리다이렉트)을 막기 위해 앱 내부 경로만 허용한다. */
export function safeNext(next: string | null, fallback = '/'): string {
  if (!next || !next.startsWith('/') || next.startsWith('//')) {
    return fallback
  }
  return next
}
