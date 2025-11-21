import type { Edge, Node } from '@xyflow/react'

import { MockAdapterType } from '@/__test-utils__/adapters/types.ts'
import { MOCK_NODE_ADAPTER, MOCK_NODE_DEVICE } from '@/__test-utils__/react-flow/nodes.ts'
import { ReactFlowTesting } from '@/__test-utils__/react-flow/ReactFlowTesting.tsx'
import type { DomainTagList } from '@/api/__generated__'
import {
  MOCK_DEVICE_TAG_JSON_SCHEMA_SIMULATION,
  MOCK_DEVICE_TAGS,
  mockAdapter,
  mockProtocolAdapter,
} from '@/api/hooks/useProtocolAdapters/__handlers__'
import DevicePropertyDrawer from '@/modules/Workspace/components/drawers/DevicePropertyDrawer.tsx'

const getWrapperWith = (initialNodes?: Node[], initialEdges?: Edge[]) => {
  const Wrapper: React.JSXElementConstructor<{ children: React.ReactNode }> = ({ children }) => {
    return (
      <ReactFlowTesting
        config={{
          initialState: {
            nodes: initialNodes,
            edges: initialEdges,
          },
        }}
      >
        {children}
      </ReactFlowTesting>
    )
  }

  return Wrapper
}

describe('DevicePropertyDrawer', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
    cy.intercept('/api/v1/management/protocol-adapters/types', { items: [mockProtocolAdapter] }).as('getProtocols')
    cy.intercept('api/v1/management/protocol-adapters/adapters', { items: [mockAdapter] }).as('getAdapters')
  })

  it('should render an error message', () => {
    cy.mountWithProviders(
      <DevicePropertyDrawer
        nodeId="adapter@fgffgf"
        selectedNode={{ ...MOCK_NODE_DEVICE, position: { x: 50, y: 100 } }}
        isOpen={true}
        onClose={cy.stub}
        onEditEntity={cy.stub}
      />,
      { wrapper: getWrapperWith() }
    )

    cy.get('[role="alert"]')
      .should('have.attr', 'data-status', 'error')
      .should('contain.text', 'The protocol adapter for this device cannot be found')
  })

  it('should be accessible', () => {
    const mockResponse: DomainTagList = { items: MOCK_DEVICE_TAGS('s7-1', MockAdapterType.SIMULATION) }
    cy.intercept('/api/v1/management/protocol-adapters/adapters/*/tags', mockResponse)
    cy.intercept('/api/v1/management/protocol-adapters/tag-schemas/simulation', MOCK_DEVICE_TAG_JSON_SCHEMA_SIMULATION)

    const onClose = cy.stub().as('onClose')
    const onEditEntity = cy.stub().as('onEditEntity')

    cy.injectAxe()
    cy.mountWithProviders(
      <DevicePropertyDrawer
        nodeId="adapter@fgffgf"
        selectedNode={{ ...MOCK_NODE_DEVICE, position: { x: 50, y: 100 } }}
        isOpen={true}
        onClose={onClose}
        onEditEntity={onEditEntity}
      />,
      {
        wrapper: getWrapperWith(
          [
            { ...MOCK_NODE_DEVICE, position: { x: 50, y: 100 } },
            { ...MOCK_NODE_ADAPTER, position: { x: 50, y: 100 } },
          ],
          [{ id: 'e1', source: 'idAdapter', target: 'idDevice' }]
        ),
      }
    )

    cy.get('@onClose').should('not.have.been.called')
    cy.getByAriaLabel('Close').click()
    cy.get('@onClose').should('have.been.calledOnce')

    cy.get('header').should('contain.text', 'Device Overview')
    cy.get('h2').should('have.text', 'List of tags')

    cy.checkAccessibility()
  })
})
