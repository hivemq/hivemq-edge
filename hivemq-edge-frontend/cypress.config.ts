import { defineConfig } from 'cypress'
import installLogsPrinter from 'cypress-terminal-report/src/installLogsPrinter.js'
import { plugin as cypressGrepPlugin } from '@cypress/grep/plugin'

import codeCoverage from '@cypress/code-coverage/task'

export default defineConfig({
  // Cypress 15.10 deprecated the browser-readable Cypress.env() API and warns on every run while the
  // compatibility shim is on. Nothing here reads it -- code-coverage and grep both moved to `expose`
  // in the versions below -- so close the migration out now rather than inheriting a hard failure
  // when the next major drops the shim.
  allowCypressEnv: false,
  // @cypress/code-coverage v4 reads its public options from `expose`, not from the `env` bag it
  // used up to v3. Left under `env` the excludes are silently ignored and coverage instruments
  // the Cypress specs and generated clients.
  expose: {
    codeCoverage: {
      exclude: ['cypress/**/*.*', '**/__generated__/**'],
    },
  },
  retries: { runMode: 2, openMode: 0 },
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
        printLogsToConsole: 'never',
        includeSuccessfulHookLogs: false,
      })
      cypressGrepPlugin(config)
      return config
    },

    devServer: {
      framework: 'react',
      bundler: 'vite',
    },
  },
})
