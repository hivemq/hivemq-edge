import { defineConfig } from 'cypress'
import installLogsPrinter from 'cypress-terminal-report/src/installLogsPrinter.js'
import { plugin as cypressGrepPlugin } from '@cypress/grep/plugin'

import codeCoverage from '@cypress/code-coverage/task.js'

export default defineConfig({
  env: {
    codeCoverage: {
      // For centralized exclusion configuration, see coverage.config.cjs
      // Note: These exclusions are for Cypress code coverage plugin
      exclude: [
        'cypress/**/*.*',
        '**/__generated__/*',
        '**/__test-utils__/**/*',
        '**/*.cy.tsx',
        '**/*.cy.ts',
        '**/*.spec.tsx',
        '**/*.spec.ts',
        '**/*.test.tsx',
        '**/*.test.ts',
      ],
    },
    // Set CYPRESS env var to help Vite optimize for Cypress tests
    CYPRESS: true,
  },
  retries: { runMode: 2, openMode: 0 },
  // Increase timeout for module loading to handle slower dynamic imports
  defaultCommandTimeout: 10000,
  pageLoadTimeout: 100000,
  e2e: {
    video: true,
    baseUrl: 'http://localhost:3000',
    setupNodeEvents(on, config) {
      codeCoverage(on, config)
      cypressGrepPlugin(config)

      installLogsPrinter(on, {
        printLogsToConsole: 'never',
        includeSuccessfulHookLogs: false,
      })
      return config
    },
  },

  component: {
    video: true,

    setupNodeEvents(on, config) {
      codeCoverage(on, config)
      installLogsPrinter(on, {
        printLogsToConsole: 'onFail',
        includeSuccessfulHookLogs: false,
      })
      cypressGrepPlugin(config)
      return config
    },

    devServer: {
      framework: 'react',
      bundler: 'vite',
      viteConfig: {
        // Optimize for Cypress component testing
        server: {
          hmr: {
            overlay: false, // Disable HMR overlay during component tests
          },
        },
        optimizeDeps: {
          // Ensure Cypress dependencies are pre-bundled
          include: [
            'react',
            'react-dom',
            'react-dom/client',
            'cypress-axe',
            'cypress-each',
            '@percy/cypress',
            'cypress-real-events',
            '@cypress/code-coverage/support',
            '@cypress/grep',
          ],
        },
      },
    },
  },
})
