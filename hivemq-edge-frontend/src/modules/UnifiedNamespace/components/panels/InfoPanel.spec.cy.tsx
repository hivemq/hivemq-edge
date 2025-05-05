/// <reference types="cypress" />

import InfoPanel from './InfoPanel.tsx'

describe('InfoPanel', () => {
  beforeEach(() => {
    cy.viewport(800, 600)
  })

  it('should render properly', () => {
    cy.mountWithProviders(<InfoPanel />)

    cy.get('h2').should('contain.text', 'ISA-95 Unified Namespace')
    cy.getByTestId('namespace-info-documentation')
      .should('be.visible')
      .should('have.attr', 'target', 'hivemq:docs')
      .should('have.attr', 'href')
      .should('not.be.empty') // now test the href
      .and('contain', 'http')
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<InfoPanel />)
    cy.checkAccessibility()
    cy.percySnapshot('Component: InfoPanel')
  })
})
