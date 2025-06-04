/// <reference types="cypress" />

import DataHubPage from './DataHubPage.tsx'

describe('DataHubPage', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
    cy.intercept('/api/v1/frontend/capabilities', [])
  })

  it('should render the commercial warning', () => {
    cy.mountWithProviders(<DataHubPage />)
    cy.get('h1').should('contain.text', 'Data Hub on Edge')
    cy.get('h2').should('contain.text', 'Data Hub on Edge is available under a commercial license, please contact us.')
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<DataHubPage />)

    cy.checkAccessibility()
    cy.percySnapshot('Component: DataHubPage')
  })
})
