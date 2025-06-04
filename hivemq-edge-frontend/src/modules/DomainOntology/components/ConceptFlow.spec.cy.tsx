import ConceptFlow from '@/modules/DomainOntology/components/ConceptFlow.tsx'

describe('ConceptFlow', () => {
  beforeEach(() => {
    cy.viewport(400, 400)
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
    cy.mountWithProviders(<ConceptFlow />)
    cy.getByTestId('loading-spinner').should('be.visible')
    cy.get('[role="alert"]')
      .should('have.attr', 'data-status', 'error')
      .should('have.text', 'Error while loading the data')
  })

  it('should render properly', () => {
    injectAllIntercept()
    cy.mountWithProviders(<ConceptFlow />)
    cy.getByTestId('edge-panel-concept-flow').should('be.visible')
  })

  it('should be accessible', () => {
    injectAllIntercept()
    cy.injectAxe()
    cy.mountWithProviders(<ConceptFlow />)

    cy.checkAccessibility(undefined)
  })
})
