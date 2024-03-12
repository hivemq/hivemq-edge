import { RouteObject } from 'react-router-dom'

import DataHubPage from './components/DataHubPage.tsx'
import PropertyPanelController from './components/controls/PropertyPanelController.tsx'
import DataHubListings from '@datahub/components/pages/DataHubListings.tsx'
import PolicyEditorLoader from '@datahub/components/pages/PolicyEditorLoader.tsx'

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
        {
          path: 'node/:type/:nodeId',
          element: <PropertyPanelController />,
        },
      ],
    },
  ],
}
