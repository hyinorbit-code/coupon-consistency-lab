import type { ReactNode } from 'react'
import { Link } from 'react-router'
import { Alert, Card, PageHeader } from '../components/ui'

function PolicyLayout({ title, children }: { title: string; children: ReactNode }) {
  return (
    <div className="space-y-5">
      <PageHeader title={title} description="시행일: 실전 이벤트 공지 시 확정" />
      <Alert kind="warning">초안입니다. 실전 이벤트(E단계) 전에 검토해 확정합니다.</Alert>
      <Card className="space-y-6 text-sm leading-relaxed text-stone-700">{children}</Card>
    </div>
  )
}

function Section({ title, children }: { title: string; children: ReactNode }) {
  return (
    <section className="space-y-2">
      <h2 className="text-base font-semibold text-stone-900">{title}</h2>
      {children}
    </section>
  )
}

export function TermsPage() {
  return (
    <PolicyLayout title="이용약관">
      <Section title="1. 서비스 소개">
        <p>Coupon Lab은 개인이 운영하는 비영리 프로젝트로, 정해진 시각에 한정 수량의 경품 코드를 선착순으로 무료 배포합니다.</p>
      </Section>
      <Section title="2. 응모 자격">
        <ul className="list-disc space-y-1 pl-5">
          <li>카카오 계정을 연동한 회원만 응모할 수 있습니다.</li>
          <li>이벤트마다 카카오 계정당 한 번만 응모할 수 있습니다.</li>
          <li>자동화 도구(매크로·봇)나 여러 계정을 이용한 부정 응모가 확인되면 당첨이 취소될 수 있습니다.</li>
        </ul>
      </Section>
      <Section title="3. 당첨과 경품">
        <ul className="list-disc space-y-1 pl-5">
          <li>당첨 순서는 서버가 응모를 처리한 순서로 정합니다.</li>
          <li>경품은 당첨 화면과 내 당첨 내역의 바코드로 제공되며, 교환 조건과 유효기간은 경품 발행처의 정책을 따릅니다.</li>
          <li>경품은 현금으로 바꾸거나 다른 사람에게 판매할 수 없습니다.</li>
        </ul>
      </Section>
      <Section title="4. 서비스 중단">
        <p>시스템 장애나 부정 응모 대응을 위해 이벤트를 중단하거나 취소할 수 있으며, 이 경우 공지합니다.</p>
      </Section>
    </PolicyLayout>
  )
}

export function PrivacyPage() {
  return (
    <PolicyLayout title="개인정보 처리방침">
      <Section title="1. 수집하는 항목">
        <ul className="list-disc space-y-1 pl-5">
          <li>이메일 가입: 이메일, 비밀번호(복원할 수 없는 해시로만 저장), 닉네임</li>
          <li>카카오 연동: 카카오 회원번호, 닉네임</li>
          <li>휴대폰 번호는 수집하지 않습니다.</li>
        </ul>
      </Section>
      <Section title="2. 이용 목적">
        <p>회원 식별과 로그인, 이벤트 응모 자격 확인(카카오 계정당 1회), 당첨 경품 제공, 부정 응모 방지에만 사용합니다.</p>
      </Section>
      <Section title="3. 보관 기간과 파기">
        <p>회원 탈퇴 시 또는 이벤트 종료 후 정해진 기간이 지나면 지체 없이 파기하거나 다시 알아볼 수 없도록 익명화합니다. 구체적인 기간은 실전 이벤트 공지에서 확정합니다.</p>
      </Section>
      <Section title="4. 제3자 제공">
        <p>수집한 개인정보를 제3자에게 제공하지 않습니다. 경품 코드는 서비스 화면으로만 제공합니다.</p>
      </Section>
      <Section title="5. 문의">
        <p>
          개인정보 관련 문의는 운영자 이메일로 받습니다. <Link to="/" className="underline">홈으로</Link>
        </p>
      </Section>
    </PolicyLayout>
  )
}
