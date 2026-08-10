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
      forceBuildInstrument: process.env.VITE_COVERAGE === 'true',
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
        configure: (proxy) => {
          proxy.on('error', () => {
            // do nothing
          })
        },
      },
      '/module': {
        target: 'http://127.0.0.1:8080/',
        changeOrigin: true,
        secure: false,
      },
    },
    watch: {
      ignored: [path.resolve(__dirname, './coverage*/**')],
    },
  },

  // Vite optimises dependencies as it discovers them. When a Cypress component run starts with a
  // cold cache, a spec that is the first to import one of these triggers a re-optimisation, and the
  // reload that follows kills whichever spec is in flight ("Failed to fetch dynamically imported
  // module"). Each react-icons pack is a separate entry, so they are only found once a spec uses
  // one. Declare them up front and the optimiser settles before the run starts.
  optimizeDeps: {
    include: [
      '@chakra-ui/skip-nav',
      '@mswjs/data',
      // The package root, not a pack: the custom icons in src/components/react-icons call GenIcon.
      'react-icons',
      'react-icons/ai',
      'react-icons/bi',
      'react-icons/bs',
      'react-icons/fa',
      'react-icons/fa6',
      'react-icons/fi',
      'react-icons/go',
      'react-icons/gr',
      'react-icons/im',
      'react-icons/io',
      'react-icons/io5',
      'react-icons/lu',
      'react-icons/md',
      'react-icons/pi',
      'react-icons/ri',
      'react-icons/rx',
      'react-icons/si',
      'react-icons/tb',
      'react-icons/vsc',
    ],
  },

  build: {
    sourcemap: true,
    rollupOptions: {
      output: {
        // Rolldown splits shared code far more eagerly than Rollup did: the login page ends up
        // preloading dozens of chunks, most of them a couple of kilobytes, which costs enough
        // first-contentful-paint to drop the Lighthouse budget. Merge the small ones back.
        codeSplitting: {
          groups: [{ name: 'initial', tags: ['$initial'] }],
        },
      },
    },
  },
})
