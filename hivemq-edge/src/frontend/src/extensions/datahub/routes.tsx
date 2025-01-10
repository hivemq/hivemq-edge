/* eslint-disable react-refresh/only-export-components */
import { lazy } from 'react'
import { RouteObject } from 'react-router-dom'

const DataHubPage = lazy(() => import('@/extensions/datahub/components/DataHubPage.tsx'))
const PolicyEditorLoader = lazy(() => import('@datahub/components/pages/PolicyEditorLoader.tsx'))
const DataHubListings = lazy(() => import('@datahub/components/pages/DataHubListings.tsx'))
const PropertyPanelController = lazy(() => import('@datahub/components/controls/PropertyPanelController.tsx'))
const DryRunPanelController = lazy(() => import('@datahub/components/controls/DryRunPanelController.tsx'))

export const dataHubRoutes: RouteObject = {
  path: 'datahub/',
  element: <DataHubPage />,
  children: [
    {
      path: '',
      index: true,
      element: <DataHubListings />,
    },
    {
      path: ':policyType/:policyId?',
      element: <PolicyEditorLoader />,
      children: [
        { path: 'validation/', element: <DryRunPanelController /> },
        {
          path: 'node/:type/:nodeId',
          element: <PropertyPanelController />,
        },
      ],
    },
  ],
}
