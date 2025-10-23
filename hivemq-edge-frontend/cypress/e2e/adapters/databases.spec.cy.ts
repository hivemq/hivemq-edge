import { MOCK_PROTOCOL_DATABASES } from '@/__test-utils__/adapters/databases.ts'
import { loginPage, adapterPage } from 'cypress/pages'

import { cy_interceptCoreE2E } from 'cypress/utils/intercept.utils.ts'

describe('Databases Protocol Adapter', () => {
  beforeEach(() => {
    cy_interceptCoreE2E()

    cy.intercept('/api/v1/management/protocol-adapters/types', { items: [MOCK_PROTOCOL_DATABASES] }).as('getProtocols')

    loginPage.visit('/app/protocol-adapters/catalog/new/databases')
    loginPage.loginButton.click()
    adapterPage.navLink.click()
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.checkAccessibility()

    adapterPage.addNewAdapter.click()
    cy.checkAccessibility()
  })
})
