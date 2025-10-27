import { sentryVitePlugin } from '@sentry/vite-plugin'
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import * as path from 'path'
import istanbul from 'vite-plugin-istanbul'

// https://vitejs.dev/config/
export default defineConfig({
  base: './',
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
      '@datahub': path.resolve(__dirname, './src/extensions/datahub'),
      '@cypr': path.resolve(__dirname, './cypress'),
    },
  },

  plugins: [
    react(),
    sentryVitePlugin({
      org: 'hivemq',
      project: 'edge',
    }),
    istanbul({
      requireEnv: false,
      cypress: true,
    }),
  ],

  server: {
    open: true,
    port: 3000,
    // Fixes for Cypress intermittent "Failed to fetch dynamically imported module" errors
    hmr: {
      // Disable HMR overlay during Cypress tests to prevent race conditions
      overlay: process.env.CYPRESS ? false : true,
    },
    fs: {
      // More strict caching to prevent module resolution issues
      strict: false,
    },
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:8080/',
        changeOrigin: true,
        secure: false,
        // configure: (proxy, _options) => {
        //   proxy.on('error', (err, _req, _res) => {
        //     console.log('proxy error', err, _req)
        //   })
        //   proxy.on('proxyReq', (proxyReq, req, _res) => {
        //     console.log('Sending Request to the Target:', req.method, req.url, proxyReq.path)
        //   })
        //   proxy.on('proxyRes', (proxyRes, req, _res) => {
        //     console.log('Received Response from the Target:', proxyRes.statusCode, req.url)
        //   })
        // },
      },
    },
    watch: {
      ignored: [path.resolve(__dirname, './coverage*/**')],
    },
  },

  optimizeDeps: {
    // Force pre-bundling of Cypress support files to prevent dynamic import issues
    include: [
      'cypress-axe',
      'cypress-each',
      '@percy/cypress',
      'cypress-real-events',
      '@cypress/code-coverage/support',
      '@cypress/grep',
    ],
    // Ensure dependencies are properly processed during Cypress tests
    force: process.env.CYPRESS === 'true',
  },

  build: {
    sourcemap: true,
  },
})
