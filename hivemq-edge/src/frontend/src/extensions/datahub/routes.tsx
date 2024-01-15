/* eslint-disable react-refresh/only-export-components */
import { RouteObject } from 'react-router-dom'

import DataHubPage from './components/DataHubPage.tsx'
import PolicyTable from './components/pages/PolicyTable.tsx'
import PolicyEditor from './components/pages/PolicyEditor.tsx'
import PropertyPanelController from './components/controls/PropertyPanelController.tsx'

export const dataHubRoutes: RouteObject = {
  path: 'datahub/',
  element: <DataHubPage />,
  children: [
    {
      path: '',
      index: true,
      element: <PolicyTable />,
    },
    {
      path: ':policyType/:policyId',
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
