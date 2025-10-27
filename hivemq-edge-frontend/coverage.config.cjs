/**
 * Coverage Configuration
 *
 * Central place to configure which files/folders to exclude from coverage.
 * This is used by all coverage scripts and NYC configurations.
 */

module.exports = {
  /**
   * Folders and patterns to exclude from coverage
   *
   * Add patterns here to exclude from all coverage reports.
   * Uses glob patterns.
   */
  exclude: [
    // Test files
    'cypress/**/*.*',
    '**/*.cy.tsx',
    '**/*.cy.ts',
    '**/*.spec.tsx',
    '**/*.spec.ts',
    '**/*.test.tsx',
    '**/*.test.ts',

    // Type definitions
    '**/*.d.ts',

    // Test utilities
    'src/__test-utils__/**/*',

    // Mock service worker
    'public/mockServiceWorker.js',

    // Build output
    'dist/**/*',
    'build/**/*',

    // Add your custom exclusions below:
    // 'src/legacy/**/*',
    // 'src/experimental/**/*',
  ],

  /**
   * Files to include in coverage
   */
  include: ['src/**/*.ts', 'src/**/*.tsx'],

  /**
   * Coverage thresholds (optional - currently disabled during merge)
   */
  thresholds: {
    lines: 90,
    functions: 90,
    branches: 90,
    statements: 90,
  },
}
