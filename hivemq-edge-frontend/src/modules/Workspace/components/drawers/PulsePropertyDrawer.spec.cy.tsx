/// <reference types="cypress" />

import { MOCK_CAPABILITIES } from '@/api/hooks/useFrontendServices/__handlers__'
import { MOCK_PULSE_ASSET_LIST } from '@/api/hooks/usePulse/__handlers__'
import PulsePropertyDrawer from '@/modules/Workspace/components/drawers/PulsePropertyDrawer.tsx'
import type { NodePulseType } from '@/modules/Workspace/types.ts'
import { NodeTypes } from '@/modules/Workspace/types.ts'

const mockNode: NodePulseType = {
  position: { x: 0, y: 0 },
  id: 'pulseId',
  type: NodeTypes.PULSE_NODE,
  data: { label: 'my assets', id: 'pulseId' },
}

describe('PulsePropertyDrawer', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
  })

  it('should render errors', () => {
    cy.intercept('/api/v1/frontend/capabilities', { items: [] })

    const onClose = cy.stub()
    cy.mountWithProviders(
      <PulsePropertyDrawer nodeId="pulseId" selectedNode={mockNode} isOpen={true} onClose={onClose} />
    )

    cy.get('header').should('contain.text', 'Pulse Agent Overview')
    cy.get('h2').should('contain.text', 'Pulse Assets are not yet available for HiveMQ Edge.')
  })

  it('should render properly', () => {
    cy.intercept('/api/v1/frontend/capabilities', MOCK_CAPABILITIES)
    cy.intercept('/api/v1/management/pulse/managed-assets', MOCK_PULSE_ASSET_LIST).as('assets')
    cy.intercept('/api/v1/management/pulse/asset-mappers', { statusCode: 202, log: false })

    const onClose = cy.stub().as('onClose')
    cy.mountWithProviders(
      <PulsePropertyDrawer nodeId="pulseId" selectedNode={mockNode} isOpen={true} onClose={onClose} />
    )

    cy.wait('@assets')

    cy.get('@onClose').should('not.have.been.called')
    cy.getByAriaLabel('Close').click()
    cy.get('@onClose').should('have.been.calledOnce')

    cy.get('header').should('contain.text', 'Pulse Agent Overview')
    cy.getByTestId('table-container').within(() => {
      // check the table is in "summary" mode
      cy.get('table thead tr th').should('have.length', 4)
    })
  })

  it('should be accessible', () => {
    cy.intercept('/api/v1/frontend/capabilities', MOCK_CAPABILITIES)
    cy.intercept('/api/v1/management/pulse/managed-assets', MOCK_PULSE_ASSET_LIST)
    cy.intercept('/api/v1/management/pulse/asset-mappers', { statusCode: 202, log: false })

    cy.injectAxe()
    cy.mountWithProviders(
      <PulsePropertyDrawer nodeId="pulseId" selectedNode={mockNode} isOpen={true} onClose={cy.stub} />
    )

    cy.checkAccessibility()
  })
})
