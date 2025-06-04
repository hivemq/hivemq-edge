/// <reference types="cypress" />
import LicenseWarning from '@datahub/components/helpers/LicenseWarning.tsx'

describe('LicenseWarning', () => {
  beforeEach(() => {
    cy.viewport(800, 500)
  })

  it('should renders properly', () => {
    cy.mountWithProviders(<LicenseWarning />)
    cy.get('img').should('have.attr', 'alt', 'Data Hub on Edge')
    cy.get('h2').should('contain.text', 'Data Hub on Edge is available under a commercial license, please contact us.')
    cy.get('h2').find('a').should('have.attr', 'href', 'https://www.hivemq.com/contact/')
  })
})
