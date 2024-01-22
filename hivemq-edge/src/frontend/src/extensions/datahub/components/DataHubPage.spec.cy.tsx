/// <reference types="cypress" />

import DataHubPage from '@/extensions/datahub/components/DataHubPage.tsx'

describe('DataHubPage', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
    cy.intercept('/api/v1/frontend/capabilities', [])
    // cy.intercept('/api/v1/management/events?*', []).as('getEvents')
  })

  it('should render the commercial warning', () => {
    cy.mountWithProviders(<DataHubPage />)
    cy.get('h1').should('contain.text', 'Data Hub')
    cy.get('h2').should('contain.text', 'Datahub is now available to commercial licenses')
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<DataHubPage />)

    cy.checkAccessibility()
    cy.percySnapshot('Component: DataHubPage')
  })
})
