import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// 운영에서는 CloudFront가 /api/*를 백엔드로 보낸다. 로컬에서는 이 프록시가 같은 역할을 해서
// 브라우저 입장에서 화면과 API가 같은 출처가 된다(ADR-0011).
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
})
