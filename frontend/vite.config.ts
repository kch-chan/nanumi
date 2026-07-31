import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import tailwindcss from '@tailwindcss/vite';

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    // 개발 서버에서 /api 요청을 백엔드(Spring Boot, 8080)로 프록시
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
});
