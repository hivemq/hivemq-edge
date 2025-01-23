import type { Edge, Node } from 'reactflow'
import { CustomNodeTesting } from '@/__test-utils__/react-flow/CustomNodeTesting.tsx'
import { mockProtocolAdapter } from '@/api/hooks/useProtocolAdapters/__handlers__'
import { MOCK_CAPABILITIES } from '@/api/hooks/useFrontendServices/__handlers__'
import { EdgeTypes, NodeTypes } from '@/modules/Workspace/types.ts'

import MonitoringEdge from './MonitoringEdge.tsx'
import { NodeAdapter } from '@/modules/Workspace/components/nodes'

const MOCK_NODES: Node[] = [
  {
    id: 'idAdapter',
    data: { label: 'From here' },
    position: { x: 0, y: 0 },
    type: 'input',
  },

  {
    id: 'idAdapter2',
    data: { label: 'To there' },
    position: { x: 100, y: 300 },
    type: 'output',
  },
]

const MOCK_EDGE_ID = 'edge-test'
const MOCK_EDGE: Edge[] = [
  {
    id: MOCK_EDGE_ID,
    source: 'idAdapter',
    target: 'idAdapter2',
    type: EdgeTypes.REPORT_EDGE,
  },
]

describe('MonitoringEdge', () => {
  beforeEach(() => {
    cy.viewport(400, 450)
    cy.intercept('/api/v1/frontend/capabilities', MOCK_CAPABILITIES)
    cy.intercept('/api/v1/management/protocol-adapters/types', { items: [mockProtocolAdapter] })
    cy.intercept('/api/v1/data-hub/data-validation/policies', [])
  })

  it('should render the observability CTA', () => {
    cy.mountWithProviders(
      <CustomNodeTesting
        nodes={MOCK_NODES}
        edges={MOCK_EDGE}
        edgeTypes={{ [EdgeTypes.REPORT_EDGE]: MonitoringEdge }}
        nodeTypes={{
          [NodeTypes.ADAPTER_NODE]: NodeAdapter,
        }}
      />
    )
    cy.getByTestId('observability-panel-trigger').should('have.attr', 'aria-label', 'Open the Observability panel')
    cy.getByTestId('test-navigate-pathname').should('have.text', '/')
    cy.getByTestId('observability-panel-trigger').click()
    cy.getByTestId('test-navigate-pathname').should('have.text', `/workspace/link/${MOCK_EDGE_ID}`)
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(
      <CustomNodeTesting nodes={MOCK_NODES} edges={MOCK_EDGE} edgeTypes={{ [EdgeTypes.REPORT_EDGE]: MonitoringEdge }} />
    )
    cy.checkAccessibility(undefined, {
      rules: {
        // ReactFlow watermark not accessible
        'color-contrast': { enabled: false },
      },
    })
    cy.percySnapshot('Component: MonitoringEdge')
  })
})
