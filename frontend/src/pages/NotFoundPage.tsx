import { LinkButton } from '../components/ui'

export function NotFoundPage() {
  return (
    <div className="space-y-4 pt-12 text-center">
      <p className="text-5xl font-extrabold text-stone-300">404</p>
      <p className="text-stone-600">페이지를 찾을 수 없어요.</p>
      <LinkButton to="/" variant="secondary">
        홈으로
      </LinkButton>
    </div>
  )
}
