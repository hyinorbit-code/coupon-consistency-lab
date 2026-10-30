import { BrowserRouter, Navigate, Route, Routes } from 'react-router'
import { AuthProvider } from './auth/AuthProvider'
import { ExperimentLoginPage } from './pages/ExperimentLoginPage'
import { MePage } from './pages/MePage'

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <main>
          <Routes>
            <Route path="/login" element={<ExperimentLoginPage />} />
            <Route path="/me" element={<MePage />} />
            <Route path="*" element={<Navigate to="/login" replace />} />
          </Routes>
        </main>
      </BrowserRouter>
    </AuthProvider>
  )
}
