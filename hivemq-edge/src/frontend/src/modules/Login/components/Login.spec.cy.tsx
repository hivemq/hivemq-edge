/// <reference types="cypress" />

import Login from '@/modules/Login/components/Login.tsx'

describe('Login', () => {
  beforeEach(() => {
    cy.viewport(800, 900)
  })

  it('should render properly', () => {
    cy.intercept('/api/v1/auth/authenticate', {
      token: 'fake_token',
    }).as('getConfig')

    cy.mountWithProviders(<Login />)
    // cy.get('.cxccc').should('contain.text', 'xxx')
    cy.get('#username').type('123')
    cy.get('#password').type('abc')

    cy.getByTestId('loginPage-submit').click()

    cy.wait('@getConfig').then(() => {
      cy.get("[role='alert']")
        .should('contain.text', 'Authentication Token')
        .should('contain.text', 'Your credentials have expired. Contact us')
    })
  })

  it.skip('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<Login />)
    cy.checkAccessibility()
    cy.percySnapshot('Component: Login')
  })
})
