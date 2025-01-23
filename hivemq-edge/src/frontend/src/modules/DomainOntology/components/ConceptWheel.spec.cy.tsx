import ConceptWheel from '@/modules/DomainOntology/components/ConceptWheel.tsx'

describe('ConceptWheel', () => {
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
    cy.mountWithProviders(<ConceptWheel />)
    cy.getByTestId('loading-spinner').should('be.visible')
    cy.get('[role="alert"]')
      .should('have.attr', 'data-status', 'error')
      .should('have.text', 'Error while loading the data')
  })

  it('should render properly', () => {
    injectAllIntercept()
    cy.mountWithProviders(<ConceptWheel />)
    cy.getByTestId('edge-panel-concept-wheel').should('be.visible')
  })

  it('should be accessible', () => {
    injectAllIntercept()
    cy.injectAxe()
    cy.mountWithProviders(<ConceptWheel />)

    cy.checkAccessibility()
  })
})
