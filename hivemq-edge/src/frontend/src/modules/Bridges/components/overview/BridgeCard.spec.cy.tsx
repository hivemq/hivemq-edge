/// <reference types="cypress" />

import { MOCK_BRIDGE_ID } from '@/__test-utils__/mocks.ts'
import { mockBridgeConnectionStatus } from '@/api/hooks/useConnection/__handlers__'
import { mockBridge } from '@/api/hooks/useGetBridges/__handlers__'

import BridgeCard from './BridgeCard.tsx'

describe('BridgeCard', () => {
  beforeEach(() => {
    cy.viewport(500, 800)
  })

  it('should render properly', () => {
    cy.intercept('/api/v1/management/bridges/status', { items: [mockBridgeConnectionStatus] }).as('getStatus')
    const mockOnSubmit = cy.stub().as('onNavigate')

    cy.mountWithProviders(<BridgeCard {...mockBridge} id={MOCK_BRIDGE_ID} onNavigate={mockOnSubmit} />)

    cy.wait('@getStatus')

    cy.getByTestId('bridge-name').should('contain.text', MOCK_BRIDGE_ID)
    cy.getByAriaLabel('Edit').click()
    cy.get('@onNavigate').should('be.calledWith', `/mqtt-bridges/${MOCK_BRIDGE_ID}`)
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<BridgeCard {...mockBridge} />)
    cy.checkAccessibility()
    cy.percySnapshot('Component: BridgeCard')
  })
})
