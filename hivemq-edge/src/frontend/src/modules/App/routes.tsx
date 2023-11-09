import { createBrowserRouter } from 'react-router-dom'

import Dashboard from '../Dashboard/Dashboard.tsx'
import ErrorPage from './components/ErrorPage.tsx'
import PageContainer from '../../components/PageContainer.tsx'

import BridgePage from '@/modules/Bridges/BridgePage.tsx'
import ProtocolAdapterPage from '@/modules/ProtocolAdapters/ProtocolAdapterPage.tsx'
import BridgeEditor from '@/modules/Bridges/components/panels/BridgeEditor.tsx'
import AdapterController from '@/modules/ProtocolAdapters/components/AdapterController.tsx'
import UnifiedNamespacePage from '@/modules/UnifiedNamespace/UnifiedNamespacePage.tsx'
import WelcomePage from '@/modules/Welcome/WelcomePage.tsx'
import LoginPage from '@/modules/Login/LoginPage.tsx'
import UnifiedNamespaceEditor from '@/modules/UnifiedNamespace/components/UnifiedNamespaceEditor.tsx'
import EdgeFlowPage from '@/modules/EdgeVisualisation/EdgeFlowPage.tsx'
import NodePropertyDrawer from '@/modules/EdgeVisualisation/components/drawers/NodePropertyDrawer.tsx'
import EvenLogPage from '@/modules/EventLog/EvenLogPage.tsx'

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
              element: <NodePropertyDrawer />,
            },
          ],
        },
        {
          path: 'event-logs/',
          element: <EvenLogPage />,
        },
        {
          path: 'modules/',
          element: <PageContainer />,
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
      ],
    },
    {
      path: '/login',
      element: <LoginPage />,
    },
  ],
  { basename: '/app' }
)
