import { defineConfig } from 'cypress'
import installLogsPrinter from 'cypress-terminal-report/src/installLogsPrinter.js'
import { plugin as cypressGrepPlugin } from '@cypress/grep/plugin'

import codeCoverage from '@cypress/code-coverage/task.js'

export default defineConfig({
  env: {
    codeCoverage: {
      exclude: ['cypress/**/*.*', '**/__generated__/*'],
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
