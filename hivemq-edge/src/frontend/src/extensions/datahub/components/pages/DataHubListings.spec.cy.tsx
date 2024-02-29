/// <reference types="cypress" />

import DataHubListings from '@datahub/components/pages/DataHubListings.tsx'

describe('DataHubListings', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
  })

  it('should render the tabs', () => {
    cy.mountWithProviders(<DataHubListings />)

    cy.getByTestId('list-tabs').find('button[role="tab"]').as('tabs')
    cy.get('@tabs').should('have.length', 3)
    cy.get('@tabs').eq(0).should('have.text', 'Policies')
    cy.get('@tabs').eq(1).should('have.text', 'Schemas')
    cy.get('@tabs').eq(2).should('have.text', 'Scripts')
    cy.get("[role='tabpanel']").should('have.length', 3)
  })

  it('should navigate through the tabs', () => {
    cy.mountWithProviders(<DataHubListings />)

    cy.getByTestId('list-tabs').find('button[role="tab"]').as('tabs')
    cy.get('@tabs').eq(0).click()
    cy.get("[role='tabpanel']").eq(0).should('contain.text', 'This is where all your policies will be listed')

    cy.get('@tabs').eq(1).click()
    cy.get("[role='tabpanel']")
      .eq(1)
      .should('contain.text', 'All your schemas used for validation or transformation will be listed there')

    cy.get('@tabs').eq(2).click()
    cy.get("[role='tabpanel']").eq(2).should('contain.text', 'Your transformation code will be listed there')
  })
})
