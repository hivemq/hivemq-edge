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
      '@cypr': path.resolve(__dirname, './cypress'),
    },
  },
  test: {
    globals: true,
    environment: 'jsdom',
    setupFiles: './src/__test-utils__/setup.ts',
    coverage: {
      include: ['**/src/'],
      // For centralized exclusion configuration, see coverage.config.cjs
      // Note: Vitest requires explicit exclude patterns (doesn't support external config)
      exclude: [
        // Generated code
        '**/src/api/__generated__/**',

        // Test utilities and mocks
        '**/__handlers__/**',
        '**/__test-utils__/**',

        // Test files
        '**/*.cy.tsx',
        '**/*.cy.ts',
        '**/*.spec.tsx',
        '**/*.spec.ts',
        '**/*.test.tsx',
        '**/*.test.ts',

        // Type definitions
        '**/types.ts',
        '**/*.d.ts',

        // Theme (if needed)
        '**/src/modules/Theme/**',

        // TSX files (if you want to exclude React components)
        '**/*.tsx',

        // Add custom exclusions here or in coverage.config.cjs
      ],
      provider: 'istanbul', // or 'v8'
      reporter: ['text', 'json', 'html', 'lcov'],
      reportsDirectory: './coverage-vitest/',
    },
  },
})
