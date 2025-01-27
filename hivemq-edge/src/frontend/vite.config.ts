import { sentryVitePlugin } from '@sentry/vite-plugin'
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import * as path from 'path'
import istanbul from 'vite-plugin-istanbul'

// https://vitejs.dev/config/
export default defineConfig({
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
      '@datahub': path.resolve(__dirname, './src/extensions/datahub'),
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
  },

  build: {
    sourcemap: true,
  },
})
