/// <reference types="cypress" />

import NodePropertyDrawer from '@/modules/EdgeVisualisation/components/drawers/NodePropertyDrawer.tsx'
import { MOCK_NODE_ADAPTER } from '@/__test-utils__/react-flow/nodes.ts'
import { Node } from 'reactflow'
import { Adapter, Bridge } from '@/api/__generated__'
import { NodeTypes } from '@/modules/EdgeVisualisation/types.ts'

const mockNode: Node<Bridge | Adapter> = {
  position: { x: 0, y: 0 },
  id: 'adapter@fgffgf',
  type: NodeTypes.ADAPTER_NODE,
  data: MOCK_NODE_ADAPTER,
}

describe('NodePropertyDrawer', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(
      <NodePropertyDrawer selectedNode={mockNode} isOpen={true} onClose={cy.stub()} onEditEntity={cy.stub()} />
    )

    cy.checkAccessibility()
    cy.percySnapshot('Component: NodePropertyDrawer')
  })
})
