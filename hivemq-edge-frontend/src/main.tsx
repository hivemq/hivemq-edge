import React from 'react'
import { setupWorker } from 'msw/browser'
import ReactDOM from 'react-dom/client'

import { createInterceptHandlers } from '@/__test-utils__/msw/handlers.ts'
import MainApp from './modules/App/MainApp.tsx'

import config from '@/config'

import './config/sentry.config'
import './config/i18n.config'

if (config.isDevMode) {
  import(/* webpackChunkName: "hivemq-dev-chunk" */ './__test-utils__/dev-console')
}

const body = document.querySelector('body')
if (body) {
  body.dataset['appVersion'] = import.meta.env.VITE_APP_VERSION
}

const createRoot = () =>
  ReactDOM.createRoot(document.getElementById('root') as HTMLElement).render(
    <React.StrictMode>
      <MainApp />
    </React.StrictMode>
  )

if (config.features.DEV_MOCK_SERVER && config.isDevMode) {
  const worker = setupWorker(...createInterceptHandlers())
  worker.start({ onUnhandledRequest: 'bypass' }).then(() => {
    worker.listHandlers()
    createRoot()
  })
} else createRoot()

if (window.location.pathname === '/') {
  window.location.replace('/app')
}
