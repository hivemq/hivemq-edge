import { defineConfig } from 'cypress'
import installLogsPrinter from 'cypress-terminal-report/src/installLogsPrinter'

export default defineConfig({
  retries: { runMode: 2, openMode: 0 },
  e2e: {
    video: true,
    baseUrl: 'http://localhost:3000',
    setupNodeEvents(on) {
      installLogsPrinter(on, {
        printLogsToConsole: 'onFail',
        includeSuccessfulHookLogs: false,
      })
    },
  },

  component: {
    video: true,

    setupNodeEvents(on) {
      installLogsPrinter(on, {
        printLogsToConsole: 'onFail',
        includeSuccessfulHookLogs: false,
      })
    },

    devServer: {
      framework: 'react',
      bundler: 'vite',
    },
  },
})
