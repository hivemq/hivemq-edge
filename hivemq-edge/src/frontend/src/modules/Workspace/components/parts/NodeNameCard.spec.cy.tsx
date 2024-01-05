/// <reference types="cypress" />

import { Node } from 'reactflow'

import { MOCK_NODE_ADAPTER, MOCK_NODE_BRIDGE } from '@/__test-utils__/react-flow/nodes.ts'
import { Adapter, Bridge } from '@/api/__generated__'
import { NodeTypes } from '@/modules/Workspace/types.ts'

import NodeNameCard from './NodeNameCard.tsx'
import { mockProtocolAdapter } from '@/api/hooks/useProtocolAdapters/__handlers__'

const mockNodeAdapter: Node<Bridge | Adapter> = {
  position: { x: 0, y: 0 },
  id: 'adapter@fgffgf',
  type: NodeTypes.ADAPTER_NODE,
  data: MOCK_NODE_ADAPTER.data,
}

const mockNodeBridg: Node<Bridge | Adapter> = {
  position: { x: 0, y: 0 },
  id: 'adapter@fgffgf',
  type: NodeTypes.BRIDGE_NODE,
  data: MOCK_NODE_BRIDGE.data,
}

describe('NodeNameCard', () => {
  beforeEach(() => {
    cy.viewport(400, 400)
    cy.intercept('/api/v1/management/protocol-adapters/types', { items: [mockProtocolAdapter] }).as('getConfig1')
  })

  it('should render adapter properly', () => {
    cy.mountWithProviders(<NodeNameCard selectedNode={mockNodeAdapter} />)

    cy.getByTestId('node-type-icon').should('exist').should('have.attr', 'data-nodeicon', NodeTypes.ADAPTER_NODE)
    cy.getByTestId('node-type-text').should('contain.text', 'adapter')
    cy.getByTestId('node-adapter-type').should('contain.text', 'Simulated Edge Device')
    cy.getByTestId('node-name').should('contain.text', 'my-adapter')
  })

  it('should render bridge properly', () => {
    cy.mountWithProviders(<NodeNameCard selectedNode={mockNodeBridg} />)

    cy.getByTestId('node-type-icon').should('exist').should('have.attr', 'data-nodeicon', NodeTypes.BRIDGE_NODE)
    cy.getByTestId('node-type-text').should('contain.text', 'bridge')
    cy.getByTestId('node-adapter-type').should('not.exist')
    cy.getByTestId('node-name').should('contain.text', 'bridge-id-01')
  })
})
