import react from '@vitejs/plugin-react'
import { defineConfig } from 'vitest/config'
import path from 'path'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  test: {
    globals: true,
    environment: 'jsdom',
    setupFiles: './src/__test-utils__/setup.ts',
    coverage: {
      exclude: ['**/src/api/__generated__/**'],
      provider: 'istanbul', // or 'v8'
      reporter: ['text', 'json', 'html', 'lcov'],
    },
  },
})
