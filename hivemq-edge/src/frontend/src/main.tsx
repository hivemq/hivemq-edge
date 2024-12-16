import React from 'react'
import ReactDOM from 'react-dom/client'
import MainApp from './modules/App/MainApp.tsx'
import config from '@/config'

import './config/sentry.config'
import './config/i18n.config'
import { ConditionalWrapper } from '@/components/ConditonalWrapper.tsx'
import { PrivateMqttClientProvider } from '@/hooks/usePrivateMqttClient/PrivateMqttClientProvider.tsx'

if (config.isDevMode) {
  import(/* webpackChunkName: "hivemq-dev-chunk" */ './__test-utils__/dev-console')
}

const body = document.querySelector('body')
if (body) {
  body.dataset['appVersion'] = import.meta.env.VITE_APP_VERSION
}

ReactDOM.createRoot(document.getElementById('root') as HTMLElement).render(
  <React.StrictMode>
    <ConditionalWrapper
      condition={import.meta.env.VITE_FLAG_MOCK_SERVER === 'true'}
      wrapper={(children) => <PrivateMqttClientProvider>{children}</PrivateMqttClientProvider>}
    >
      <MainApp />
    </ConditionalWrapper>
  </React.StrictMode>
)

if (window.location.pathname === '/') {
  window.location.replace('/app')
}
