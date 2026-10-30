import { lazy, Suspense } from 'react'
import { BrowserRouter, Route, Routes } from 'react-router'
import { AuthProvider } from './auth/AuthProvider'
import { AdminLayout } from './components/AdminLayout'
import { RequireAdmin, RequireAuth } from './components/guards'
import { Layout } from './components/Layout'
import { mocksEnabled } from './mocks/enabled'
import { AdminDropDetailPage } from './pages/admin/AdminDropDetailPage'
import { AdminDropNewPage } from './pages/admin/AdminDropNewPage'
import { AdminDropsPage } from './pages/admin/AdminDropsPage'
import { DropDetailPage } from './pages/DropDetailPage'
import { EntryResultPage } from './pages/EntryResultPage'
import { ExperimentLoginPage } from './pages/ExperimentLoginPage'
import { HomePage } from './pages/HomePage'
import { KakaoCallbackPage } from './pages/KakaoCallbackPage'
import { LoginPage } from './pages/LoginPage'
import { MyPage } from './pages/MyPage'
import { MyPrizesPage } from './pages/MyPrizesPage'
import { NotFoundPage } from './pages/NotFoundPage'
import { PrivacyPage, TermsPage } from './pages/PolicyPages'
import { SignupDonePage } from './pages/SignupDonePage'
import { SignupPage } from './pages/SignupPage'
import { VerifyEmailPage } from './pages/VerifyEmailPage'

// 개발 패널은 목업 모드에서만 불러온다.
const DevPanel = lazy(() => import('./components/DevPanel').then((module) => ({ default: module.DevPanel })))

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route element={<Layout />}>
            <Route index element={<HomePage />} />
            <Route path="drops/:id" element={<DropDetailPage />} />
            <Route path="drops/:id/result" element={<EntryResultPage />} />
            <Route path="login" element={<LoginPage />} />
            <Route path="signup" element={<SignupPage />} />
            <Route path="signup/done" element={<SignupDonePage />} />
            <Route path="verify-email" element={<VerifyEmailPage />} />
            <Route path="auth/kakao/callback" element={<KakaoCallbackPage />} />
            <Route path="experiment-login" element={<ExperimentLoginPage />} />
            <Route path="my" element={<RequireAuth><MyPage /></RequireAuth>} />
            <Route path="my/prizes" element={<RequireAuth><MyPrizesPage /></RequireAuth>} />
            <Route path="terms" element={<TermsPage />} />
            <Route path="privacy" element={<PrivacyPage />} />
            <Route path="*" element={<NotFoundPage />} />
          </Route>
          <Route path="admin" element={<RequireAdmin><AdminLayout /></RequireAdmin>}>
            <Route index element={<AdminDropsPage />} />
            <Route path="drops/new" element={<AdminDropNewPage />} />
            <Route path="drops/:id" element={<AdminDropDetailPage />} />
          </Route>
        </Routes>
        {import.meta.env.DEV && mocksEnabled && (
          <Suspense>
            <DevPanel />
          </Suspense>
        )}
      </BrowserRouter>
    </AuthProvider>
  )
}
