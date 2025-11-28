import { MOCK_PROTOCOL_MODBUS } from '@/__test-utils__/adapters/modbus.ts'
import { loginPage, adapterPage } from 'cypress/pages'

import { cy_interceptCoreE2E } from 'cypress/utils/intercept.utils.ts'

describe('Modbus Protocol Adapter', () => {
  beforeEach(() => {
    cy_interceptCoreE2E()

    cy.intercept('/api/v1/management/protocol-adapters/types', { items: [MOCK_PROTOCOL_MODBUS] }).as('getProtocols')

    loginPage.visit('/app/protocol-adapters/catalog/new/modbus')
    loginPage.loginButton.click()
    adapterPage.navLink.click()
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.checkAccessibility()

    adapterPage.addNewAdapter.click()
    cy.checkAccessibility(undefined, {
      rules: {
        'color-contrast': { enabled: false },
      },
    })
  })
})
