/// <reference types="cypress" />

import PulsePropertyDrawer from '@/modules/Workspace/components/drawers/PulsePropertyDrawer.tsx'
import { type NodeAssetsType, NodeTypes } from '@/modules/Workspace/types.ts'

const mockNode: NodeAssetsType = {
  position: { x: 0, y: 0 },
  id: 'adapter@fgffgf',
  type: NodeTypes.ASSETS_NODE,
  data: { label: 'my assets' },
}

describe('PulsePropertyDrawer', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
  })

  it('should render properly', () => {
    const onClose = cy.stub().as('onClose')
    const onEditEntity = cy.stub().as('onEditEntity')
    cy.mountWithProviders(
      <PulsePropertyDrawer
        nodeId="adapter@fgffgf"
        selectedNode={mockNode}
        isOpen={true}
        onClose={onClose}
        onEditEntity={onEditEntity}
      />
    )
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(
      <PulsePropertyDrawer
        nodeId="adapter@fgffgf"
        selectedNode={mockNode}
        isOpen={true}
        onClose={cy.stub()}
        onEditEntity={cy.stub()}
      />
    )

    cy.checkAccessibility()
  })
})
