import ProtocolIntegrationStore from '@/modules/ProtocolAdapters/components/panels/ProtocolIntegrationStore.tsx'
import { mockProtocolAdapter } from '@/api/hooks/useProtocolAdapters/__handlers__'

describe('ProtocolIntegrationStore', () => {
  beforeEach(() => {
    cy.viewport(800, 900)
    cy.intercept('/api/v1/management/protocol-adapters/types', { items: [mockProtocolAdapter] }).as('getProtocols')
    //   cy.intercept('api/v1/management/protocol-adapters/adapters', { items: [mockAdapter] }).as('getAdapters')
    //   cy.intercept('api/v1/management/protocol-adapters/status', { items: [mockAdapterConnectionStatus] }).as('getStatus')
  })

  it('should handle loading errors', () => {
    cy.intercept('/api/v1/management/protocol-adapters/types', { statusCode: 404 }).as('getProtocols')
    cy.mountWithProviders(<ProtocolIntegrationStore />)

    cy.wait('@getProtocols')
    cy.get('[role="alert"]').should('have.attr', 'data-status', 'error')
    cy.get('[role="alert"] div div').eq(0).should('have.text', 'Not Found')
    cy.get('[role="alert"] div div')
      .eq(1)
      .should('have.text', 'We cannot load your adapters for the time being. Please try again later')
  })

  it.only('should render properly', () => {
    cy.mountWithProviders(<ProtocolIntegrationStore />)

    cy.wait('@getProtocols')

    cy.getByTestId('heading-protocols-list').find('h2').should('have.text', 'Protocol Adapter Catalog')
    cy.getByTestId('heading-protocols-list')
      .find('h2 + p')
      .should('have.text', 'Select the protocol to start creating your connection')

    cy.get('[role="region"]').should('have.attr', 'aria-label', 'Search and filter protocol adapters')
    cy.get('[role="region"] + [role="list"]').should('have.attr', 'aria-label', 'List of protocol adapters')
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<ProtocolIntegrationStore />)

    cy.checkAccessibility()
    cy.percySnapshot('Component: ProtocolIntegrationStore')
  })
})
