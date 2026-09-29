import { defineConfig } from 'vitest/config';

// 테스트 설정은 vite.config.ts 와 분리해 둠
// 브라우저 API(localStorage, window.location)를 쓰는 테스트가 있어서 jsdom 환경으로 돌림
export default defineConfig({
  test: {
    environment: 'jsdom',
    include: ['src/**/*.test.ts', 'src/**/*.test.tsx'],
    restoreMocks: true,
  },
});
