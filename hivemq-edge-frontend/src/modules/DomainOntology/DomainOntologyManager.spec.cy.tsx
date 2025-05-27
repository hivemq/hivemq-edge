import DomainOntologyManager from '@/modules/DomainOntology/DomainOntologyManager.tsx'

describe('DomainOntologyManager', () => {
  beforeEach(() => {
    cy.viewport(800, 500)
    cy.intercept('/api/v1/management/bridges', { items: [] })
    cy.intercept('/api/v1/management/protocol-adapters/adapters', { items: [] })
    cy.intercept('/api/v1/management/protocol-adapters/northboundMappings', { items: [] })
    cy.intercept('/api/v1/management/protocol-adapters/southboundMappings', { items: [] })
    cy.intercept('api/v1/management/protocol-adapters/tags', { items: [] })
    cy.intercept('api/v1/management/topic-filters', { items: [] })
  })

  it('should render properly', () => {
    cy.mountWithProviders(<DomainOntologyManager />)

    cy.get('[role="tablist"] [role="tab"]').as('tabs')
    cy.get('@tabs').should('have.length', 4)
    cy.get('@tabs').eq(0).should('have.text', 'Workspace Cluster').should('have.attr', 'aria-selected', 'true')
    cy.get('@tabs').eq(1).should('have.text', 'Concept Wheel').should('have.attr', 'aria-selected', 'false')
    cy.get('@tabs').eq(2).should('have.text', 'Relation Chords').should('have.attr', 'aria-selected', 'false')
    cy.get('@tabs').eq(3).should('have.text', 'Concept Flow').should('have.attr', 'aria-selected', 'false')

    cy.getByTestId('edge-panel-adapter-clusters').should('be.visible')
    cy.getByTestId('edge-panel-concept-wheel').should('not.exist')

    cy.get('@tabs').eq(1).click()

    cy.getByTestId('edge-panel-adapter-clusters').should('not.exist')
    cy.getByTestId('edge-panel-concept-wheel').should('be.visible')
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<DomainOntologyManager />)

    cy.checkAccessibility()
  })
})
