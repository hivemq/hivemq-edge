import RelationEdgeBundling from '@/modules/DomainOntology/components/RelationEdgeBundling.tsx'

describe('RelationEdgeBundling', () => {
  beforeEach(() => {
    cy.viewport(800, 400)
  })

  it('should render errors', () => {
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
