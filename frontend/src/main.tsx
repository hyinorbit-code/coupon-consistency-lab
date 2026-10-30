import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import App from './App.tsx'

// 개발 모드에서는 아직 백엔드에 없는 API를 MSW 핸들러가 가짜 응답으로 처리한다(src/api/client.ts).
createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
