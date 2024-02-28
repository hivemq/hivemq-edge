import { RouteObject } from 'react-router-dom'

import DataHubPage from './components/DataHubPage.tsx'
import PolicyEditor from './components/pages/PolicyEditor.tsx'
import PropertyPanelController from './components/controls/PropertyPanelController.tsx'
import DataHubListings from '@datahub/components/pages/DataHubListings.tsx'

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
      element: <PolicyEditor />,
      children: [
        {
          path: 'node/:type/:nodeId',
          element: <PropertyPanelController />,
        },
      ],
    },
  ],
}
