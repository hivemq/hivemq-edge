/// <reference types="cypress" />

import { MOCK_NODE_DEVICE } from '@/__test-utils__/react-flow/nodes.ts'
import DevicePropertyDrawer from '@/modules/Workspace/components/drawers/DevicePropertyDrawer.tsx'

describe('DevicePropertyDrawer', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
    cy.intercept('/api/v1/management/protocol-adapters/types', { statusCode: 404 })
  })

  it('should render properly', () => {
    const onClose = cy.stub().as('onClose')
    const onEditEntity = cy.stub().as('onEditEntity')
    cy.mountWithProviders(
      <DevicePropertyDrawer
        nodeId="adapter@fgffgf"
        selectedNode={{ ...MOCK_NODE_DEVICE, position: { x: 50, y: 100 } }}
        isOpen={true}
        onClose={onClose}
        onEditEntity={onEditEntity}
      />
    )

    cy.get('@onClose').should('not.have.been.called')
    cy.getByAriaLabel('Close').click()
    cy.get('@onClose').should('have.been.calledOnce')

    cy.get('header').should('contain.text', 'Device Overview')
  })
})
