import RelationMatrix from '@/modules/DomainOntology/components/RelationMatrix.tsx'

describe('RelationMatrix', () => {
  beforeEach(() => {
    cy.viewport(800, 400)
  })

  const injectAllIntercept = () => {
    cy.intercept('/api/v1/management/bridges', { items: [] })
    cy.intercept('/api/v1/management/protocol-adapters/northboundMappings', { items: [] })
    cy.intercept('/api/v1/management/protocol-adapters/southboundMappings', { items: [] })
    cy.intercept('api/v1/management/protocol-adapters/tags', { items: [] })
    cy.intercept('api/v1/management/topic-filters', { items: [] })
  }

  it('should render errors', () => {
    cy.intercept('/api/v1/management/bridges', { statusCode: 404 })
    cy.intercept('/api/v1/management/protocol-adapters/northboundMappings', { statusCode: 404 })
    cy.intercept('/api/v1/management/protocol-adapters/southboundMappings', { statusCode: 404 })
    cy.intercept('api/v1/management/protocol-adapters/tags', { statusCode: 404 })
    cy.intercept('api/v1/management/topic-filters', { statusCode: 404 })
    cy.mountWithProviders(<RelationMatrix />)
    cy.getByTestId('loading-spinner').should('be.visible')
    cy.get('[role="alert"]')
      .should('have.attr', 'data-status', 'error')
      .should('have.text', 'Error while loading the data')
  })

  // TODO[NVL] This test checks the "empty state"; should we test a full data payload?
  it('should render properly', () => {
    injectAllIntercept()
    cy.mountWithProviders(<RelationMatrix />)

    cy.getByTestId('edge-panel-relation-matrix').should('be.visible')
  })

  it('should be accessible', () => {
    injectAllIntercept()
    cy.injectAxe()
    cy.mountWithProviders(<RelationMatrix />)

    cy.checkAccessibility()
  })
})
