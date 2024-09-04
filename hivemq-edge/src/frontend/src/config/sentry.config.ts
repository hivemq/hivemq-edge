import * as Sentry from '@sentry/react'
import { useEffect } from 'react'
import { createRoutesFromChildren, matchRoutes, useLocation, useNavigationType } from 'react-router-dom'

if (import.meta.env.MODE !== 'development')
  Sentry.init({
    dsn: import.meta.env.VITE_MONITORING_SENTRY,
    release: `hivemq-edge@${import.meta.env.VITE_HIVEMQ_EDGE_VERSION}`,
    environment: import.meta.env.MODE,

    integrations: [
      Sentry.browserProfilingIntegration(),
      Sentry.replayIntegration(),
      // https://docs.sentry.io/platforms/javascript/guides/react/configuration/integrations/react-router/
      Sentry.reactRouterV6BrowserTracingIntegration({
        useEffect,
        useLocation,
        useNavigationType,
        createRoutesFromChildren,
        matchRoutes,
      }),
    ],
    // Set `tracePropagationTargets` to control for which URLs trace propagation should be enabled
    tracePropagationTargets: [],
    // Set tracesSampleRate to 1.0 to capture 100%
    // of transactions for tracing.
    tracesSampleRate: 1.0,

    // Capture Replay for 10% of all sessions,
    // plus for 100% of sessions with an error
    replaysSessionSampleRate: 0.1,
    replaysOnErrorSampleRate: 1.0,
  })
