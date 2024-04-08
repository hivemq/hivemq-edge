import { MOCK_NODE_ADAPTER } from '@/__test-utils__/react-flow/nodes.ts'

import { mockProtocolAdapter } from '@/api/hooks/useProtocolAdapters/__handlers__'
import { CustomNodeTesting } from '@/__test-utils__/react-flow/CustomNodeTesting.tsx'
import { EdgeTypes } from '@/modules/Workspace/types.ts'

import MonitoringEdge from './MonitoringEdge.tsx'
import { MOCK_CAPABILITIES } from '@/api/hooks/useFrontendServices/__handlers__'

describe('MonitoringEdge', () => {
  beforeEach(() => {
    cy.viewport(400, 400)
    cy.intercept('/api/v1/frontend/capabilities', MOCK_CAPABILITIES)
    cy.intercept('/api/v1/management/protocol-adapters/types', { items: [mockProtocolAdapter] })
    cy.intercept('/api/v1/data-hub/data-validation/policies', [])
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(
      <CustomNodeTesting
        nodes={[
          { ...MOCK_NODE_ADAPTER, position: { x: 0, y: 0 } },
          { ...MOCK_NODE_ADAPTER, id: 'idAdapter2', position: { x: 100, y: 300 } },
        ]}
        edges={[
          {
            id: `edge-test`,
            source: 'idAdapter',
            target: 'idAdapter2',
            type: EdgeTypes.REPORT_EDGE,
          },
        ]}
        edgeTypes={{ [EdgeTypes.REPORT_EDGE]: MonitoringEdge }}
      />
    )
    cy.checkAccessibility()
    cy.percySnapshot('Component: MonitoringEdge')
  })
})
