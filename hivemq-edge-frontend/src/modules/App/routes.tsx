import { lazy } from 'react'
import { createBrowserRouter } from 'react-router-dom'

import LoginPage from '@/modules/Login/LoginPage.tsx'
import Dashboard from '@/modules/Dashboard/Dashboard.tsx'
import ErrorPage from '@/modules/App/components/ErrorPage.tsx'

const WelcomePage = lazy(() => import('@/modules/Welcome/WelcomePage.tsx'))
const BridgePage = lazy(() => import('@/modules/Bridges/BridgePage.tsx'))
const BridgeEditorDrawer = lazy(() => import('@/modules/Bridges/components/BridgeEditorDrawer.tsx'))
const ProtocolAdapterPage = lazy(() => import('@/modules/ProtocolAdapters/ProtocolAdapterPage.tsx'))
const AdapterController = lazy(() => import('@/modules/ProtocolAdapters/components/AdapterController.tsx'))
const ExportDrawer = lazy(() => import('@/modules/ProtocolAdapters/components/drawers/ExportDrawer.tsx'))
const UnifiedNamespaceEditor = lazy(() => import('@/modules/UnifiedNamespace/components/UnifiedNamespaceEditor.tsx'))
const UnifiedNamespacePage = lazy(() => import('@/modules/UnifiedNamespace/UnifiedNamespacePage.tsx'))
const EdgeFlowPage = lazy(() => import('@/modules/Workspace/EdgeFlowPage.tsx'))
const ConfigurationPanelController = lazy(
  () => import('@/modules/Workspace/components/controls/ConfigurationPanelController.tsx')
)
const EvenLogPage = lazy(() => import('@/modules/EventLog/EvenLogPage.tsx'))
const AdapterSubscriptionManager = lazy(() => import('@/modules/Mappings/AdapterMappingManager.tsx'))
const CombinerMappingManager = lazy(() => import('../Mappings/CombinerMappingManager.tsx'))
const TopicFilterManager = lazy(() => import('@/modules/TopicFilters/TopicFilterManager.tsx'))
const ProtocolIntegrationStore = lazy(
  () => import('@/modules/ProtocolAdapters/components/panels/ProtocolIntegrationStore.tsx')
)
const ProtocolAdapters = lazy(() => import('@/modules/ProtocolAdapters/components/panels/ProtocolAdapters.tsx'))
const PulsePage = lazy(() => import('@/modules/Pulse/PulsePage.tsx'))
const ManagedAssetDrawer = lazy(() => import('@/modules/Pulse/components/assets/ManagedAssetDrawer.tsx'))

import { dataHubRoutes } from '@/extensions/datahub/routes.tsx'
import { MappingType } from '@/modules/Mappings/types.ts'
import { EdgeTypes, NodeTypes } from '@/modules/Workspace/types.ts'

import config from '@/config'

/**
 * @experimental This function is used to determine the base path for the router in production.
 */
function getBasename(): string {
  if (config.isDevMode) {
    return '/app'
  }
  const pathname = window.location.pathname
  if (pathname.endsWith('/app')) {
    return pathname.substring(0, pathname.length - 4)
  }
  const index = pathname.lastIndexOf('/app/')
  if (index !== -1) {
    return pathname.substring(0, index + 4)
  }
  return pathname
}

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
              element: <BridgeEditorDrawer isNew />,
            },
            {
              path: ':bridgeId',
              element: <BridgeEditorDrawer />,
            },
          ],
        },

        {
          path: 'protocol-adapters/',
          element: <ProtocolAdapterPage />,
          children: [
            {
              path: '',
              element: <ProtocolAdapters />,
              children: [
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
              path: 'catalog',
              element: <ProtocolIntegrationStore />,
              children: [
                {
                  path: 'new/:type',
                  element: <AdapterController isNew />,
                },
              ],
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
              path: 'edge/:edgeId',
              element: <ConfigurationPanelController type={NodeTypes.EDGE_NODE} />,
            },
            {
              path: 'pulse-agent/*',
              element: <ConfigurationPanelController type={NodeTypes.PULSE_NODE} />,
            },
            {
              path: 'device/:deviceId',
              element: <ConfigurationPanelController type={NodeTypes.DEVICE_NODE} />,
            },
            {
              path: 'group/:groupId',
              element: <ConfigurationPanelController type={NodeTypes.CLUSTER_NODE} />,
            },
            {
              path: 'bridge/:bridgeId',
              element: <ConfigurationPanelController type={NodeTypes.BRIDGE_NODE} />,
            },
            {
              path: 'adapter/:adapterType/:adapterId',
              element: <ConfigurationPanelController type={NodeTypes.ADAPTER_NODE} />,
              children: [
                {
                  path: 'northbound',
                  element: <AdapterSubscriptionManager type={MappingType.NORTHBOUND} />,
                },
                {
                  path: 'southbound',
                  element: <AdapterSubscriptionManager type={MappingType.SOUTHBOUND} />,
                },
              ],
            },

            {
              path: 'connector/:connectorId',
              element: <ConfigurationPanelController type={EdgeTypes.DYNAMIC_EDGE} />,
            },
            {
              path: 'combiner/:combinerId/:tabId?',
              element: <CombinerMappingManager />,
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
        {
          path: 'pulse-assets/',
          element: <PulsePage />,
          children: [
            {
              path: ':assetId',
              element: <ManagedAssetDrawer />,
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
  {
    basename: getBasename(),
    future: {
      v7_relativeSplatPath: true,
    },
  }
)
