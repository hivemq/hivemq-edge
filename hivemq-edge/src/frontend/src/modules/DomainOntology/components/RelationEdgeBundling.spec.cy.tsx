import RelationEdgeBundling from '@/modules/DomainOntology/components/RelationEdgeBundling.tsx'

describe('RelationEdgeBundling', () => {
  beforeEach(() => {
    cy.viewport(800, 400)
  })

  it('should render errors', () => {
    cy.intercept('/api/v1/management/bridges', { statusCode: 404 })
    cy.intercept('/api/v1/management/protocol-adapters/northboundMappings', { statusCode: 404 })
    cy.intercept('/api/v1/management/protocol-adapters/southboundMappings', { statusCode: 404 })
    cy.intercept('api/v1/management/protocol-adapters/tags', { statusCode: 404 })
    cy.intercept('api/v1/management/topic-filters', { statusCode: 404 })
    cy.mountWithProviders(<RelationEdgeBundling />)
    cy.getByTestId('loading-spinner').should('be.visible')
    cy.get('[role="alert"]')
      .should('have.attr', 'data-status', 'error')
      .should('have.text', 'Error while loading the data')
  })

  // TODO[NVL] Not yet safely implemented
  it.skip('should render properly', () => {
    cy.mountWithProviders(<RelationEdgeBundling />)
    cy.getByTestId('edge-panel-relation-edgeBundling').should('be.visible')
  })
})
