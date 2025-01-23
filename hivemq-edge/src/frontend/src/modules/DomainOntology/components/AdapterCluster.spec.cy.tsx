import AdapterCluster from '@/modules/DomainOntology/components/AdapterCluster.tsx'

describe('AdapterCluster', () => {
  beforeEach(() => {
    cy.viewport(800, 600)
  })

  const injectAllIntercept = () => {
    cy.intercept('/api/v1/management/bridges', { items: [] })
    cy.intercept('/api/v1/management/protocol-adapters/adapters', { items: [] })
  }

  it('should render errors', () => {
    cy.mountWithProviders(<AdapterCluster />)
    cy.getByTestId('loading-spinner').should('be.visible')
    cy.get('[role="alert"]')
      .should('have.attr', 'data-status', 'error')
      .should('have.text', 'Error while loading the data')
  })

  it('should render properly', () => {
    injectAllIntercept()
    cy.mountWithProviders(<AdapterCluster />)
    cy.getByTestId('edge-panel-adapter-clusters').should('be.visible')

    cy.getByTestId('chart-wrapper-header').find('button').eq(0).should('have.text', 'Configuration')
    cy.getByTestId('chart-wrapper-header').find('button').eq(1).should('have.text', 'Help')
  })

  it('should be accessible', () => {
    injectAllIntercept()
    cy.injectAxe()
    cy.mountWithProviders(<AdapterCluster />)

    cy.checkAccessibility()
  })
})
