import { defineConfig } from 'vitest/config';
import react from '@vitejs/plugin-react';
import path from 'path';

// 개발: Vite(5173)에서 /api 요청을 백엔드(8181)로 프록시 → same-origin, 쿠키(세션·XSRF) 정상 동작(CORS 불필요).
// 빌드: React 산출물을 Spring Boot static 으로 출력 → 단일 실행물.
export default defineConfig({
  plugins: [react()],
  server: {
    host: '0.0.0.0',
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8181',
        changeOrigin: true,
      },
    },
  },
  build: {
    outDir: path.resolve(__dirname, '../src/main/resources/static'),
    emptyOutDir: true,
  },
  test: {
    environment: 'node',
    include: ['src/test/**/*.test.{js,jsx}'],
  },
});
