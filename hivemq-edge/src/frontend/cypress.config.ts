import { defineConfig } from 'cypress'
import installLogsPrinter from 'cypress-terminal-report/src/installLogsPrinter.js'

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
      installLogsPrinter(on, {
        printLogsToConsole: 'onFail',
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
      return config
    },

    devServer: {
      framework: 'react',
      bundler: 'vite',
    },
  },
})
