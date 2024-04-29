/* eslint-disable react-refresh/only-export-components */
import { lazy } from 'react'
import { createBrowserRouter } from 'react-router-dom'

import LoginPage from '@/modules/Login/LoginPage.tsx'
import Dashboard from '@/modules/Dashboard/Dashboard.tsx'
import ErrorPage from '@/modules/App/components/ErrorPage.tsx'

const WelcomePage = lazy(() => import('@/modules/Welcome/WelcomePage.tsx'))
const BridgePage = lazy(() => import('@/modules/Bridges/BridgePage.tsx'))
const BridgeEditor = lazy(() => import('@/modules/Bridges/components/panels/BridgeEditor.tsx'))
const ProtocolAdapterPage = lazy(() => import('@/modules/ProtocolAdapters/ProtocolAdapterPage.tsx'))
const AdapterController = lazy(() => import('@/modules/ProtocolAdapters/components/AdapterController.tsx'))
const UnifiedNamespaceEditor = lazy(() => import('@/modules/UnifiedNamespace/components/UnifiedNamespaceEditor.tsx'))
const UnifiedNamespacePage = lazy(() => import('@/modules/UnifiedNamespace/UnifiedNamespacePage.tsx'))
const EdgeFlowPage = lazy(() => import('@/modules/Workspace/EdgeFlowPage.tsx'))
const NodePanelController = lazy(() => import('@/modules/Workspace/components/controls/NodePanelController.tsx'))
const EvenLogPage = lazy(() => import('@/modules/EventLog/EvenLogPage.tsx'))

import { dataHubRoutes } from '@/extensions/datahub/routes.tsx'

export const routes = createBrowserRouter(
  [
    {
      path: '/',
      element: <Dashboard />,
      errorElement: <ErrorPage />,
      children: [
        {
          path: '',
          index: true,
          element: <WelcomePage />,
        },
        {
          path: 'mqtt-bridges/',
          element: <BridgePage />,
          children: [
            {
              path: 'new',
              element: <BridgeEditor isNew />,
            },
            {
              path: ':bridgeId',
              element: <BridgeEditor />,
            },
          ],
        },

        {
          path: 'protocol-adapters/',
          element: <ProtocolAdapterPage />,
          children: [
            {
              path: 'new',
              element: <AdapterController isNew />,
            },
            {
              path: ':adapterId',
              element: <AdapterController />,
            },
          ],
        },
        {
          path: 'edge-flow/',
          element: <EdgeFlowPage />,
          children: [
            {
              path: ':nodeType/:nodeId',
              element: <NodePanelController />,
            },
          ],
        },
        {
          path: 'event-logs/',
          element: <EvenLogPage />,
        },
        {
          path: 'namespace/',
          element: <UnifiedNamespacePage />,
          children: [
            {
              path: 'edit',
              element: <UnifiedNamespaceEditor />,
            },
          ],
        },
        { ...dataHubRoutes },
      ],
    },
    {
      path: '/login',
      element: <LoginPage />,
    },
  ],
  { basename: '/app' }
)
