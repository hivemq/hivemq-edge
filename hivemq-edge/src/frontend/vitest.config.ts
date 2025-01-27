import react from '@vitejs/plugin-react'
import { defineConfig } from 'vitest/config'
import path from 'path'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
      '@datahub': path.resolve(__dirname, './src/extensions/datahub'),
    },
  },
  test: {
    globals: true,
    environment: 'jsdom',
    setupFiles: './src/__test-utils__/setup.ts',
    coverage: {
      include: ['**/src/'],
      exclude: [
        '**/src/api/__generated__/**',
        '**/__handlers__/**',
        '**/__test-utils__/**',
        '**/src/modules/Theme/**',
        '**/types.ts',
        '**/*.tsx',
      ],
      provider: 'istanbul', // or 'v8'
      reporter: ['text', 'json', 'html', 'lcov'],
      reportsDirectory: './coverage-vitest/',
    },
  },
})
