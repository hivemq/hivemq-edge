import MetadataExplorer from '@/modules/Workspace/components/topics/MetadataExplorer.tsx'

describe('MetadataExplorer', () => {
  beforeEach(() => {
    cy.viewport(400, 400)
  })

  it('should render properly', () => {
    cy.mountWithProviders(<MetadataExplorer topic="test" />)

    cy.get('h2').should('contain.text', 'test')
    cy.getByTestId('loading-spinner')
    cy.getByTestId('loading-spinner').should('not.exist')

    cy.get('button').should('have.attr', 'aria-label', 'Load samples').should('have.attr', 'disabled', 'disabled')

    // TODO[NVL] Cannot test MQTTClient. Need a better mock handling
    cy.get('[role="alert"]')
      .should('have.attr', 'data-status', 'error')
      .should('have.text', 'No sample could be observed for the topic filter test')
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<MetadataExplorer topic="test" />)

    cy.getByTestId('loading-spinner')
    cy.getByTestId('loading-spinner').should('not.exist')

    cy.checkAccessibility()
    cy.percySnapshot('Component: MetadataExplorer')
  })
})
