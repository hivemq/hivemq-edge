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
const ExportDrawer = lazy(() => import('@/modules/ProtocolAdapters/components/drawers/ExportDrawer.tsx'))
const UnifiedNamespaceEditor = lazy(() => import('@/modules/UnifiedNamespace/components/UnifiedNamespaceEditor.tsx'))
const UnifiedNamespacePage = lazy(() => import('@/modules/UnifiedNamespace/UnifiedNamespacePage.tsx'))
const EdgeFlowPage = lazy(() => import('@/modules/Workspace/EdgeFlowPage.tsx'))
const NodePanelController = lazy(() => import('@/modules/Workspace/components/controls/NodePanelController.tsx'))
const EvenLogPage = lazy(() => import('@/modules/EventLog/EvenLogPage.tsx'))
const AdapterSubscriptionManager = lazy(() => import('@/modules/Mappings/AdapterMappingManager.tsx'))
const TopicFilterManager = lazy(() => import('@/modules/TopicFilters/TopicFilterManager.tsx'))

import { dataHubRoutes } from '@/extensions/datahub/routes.tsx'
import { MappingType } from '@/modules/Mappings/types.ts'

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
              path: 'new/:type',
              element: <AdapterController isNew />,
            },
            {
              path: 'edit/:type/:adapterId',
              element: <AdapterController />,
            },
            {
              path: 'edit/:type/:adapterId/export',
              element: <ExportDrawer />,
            },
          ],
        },
        {
          path: 'workspace/',
          element: <EdgeFlowPage />,
          children: [
            {
              path: 'topic-filters/',
              element: <TopicFilterManager />,
            },
            {
              path: ':nodeType/:device?/:adapter?/:nodeId',
              element: <NodePanelController />,
            },
            {
              path: ':nodeType/:device?/:adapter?/:nodeId/inward',
              element: <AdapterSubscriptionManager type={MappingType.INWARD} />,
            },
            {
              path: ':nodeType/:device?/:adapter?/:nodeId/outward',
              element: <AdapterSubscriptionManager type={MappingType.OUTWARD} />,
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
