import { BrowserRouter, Link, Navigate, Route, Routes } from 'react-router'
import { AuthProvider } from './auth/AuthProvider'
import { ExperimentLoginPage } from './pages/ExperimentLoginPage'
import { MePage } from './pages/MePage'
import { SignupDonePage } from './pages/SignupDonePage'
import { SignupPage } from './pages/SignupPage'
import { VerifyEmailPage } from './pages/VerifyEmailPage'

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <nav>
          <Link to="/signup">회원가입</Link>
          <Link to="/login">실험용 로그인</Link>
          <Link to="/me">내 정보</Link>
        </nav>
        <main>
          <Routes>
            <Route path="/signup" element={<SignupPage />} />
            <Route path="/signup/done" element={<SignupDonePage />} />
            <Route path="/verify-email" element={<VerifyEmailPage />} />
            <Route path="/login" element={<ExperimentLoginPage />} />
            <Route path="/me" element={<MePage />} />
            <Route path="*" element={<Navigate to="/signup" replace />} />
          </Routes>
        </main>
      </BrowserRouter>
    </AuthProvider>
  )
}
