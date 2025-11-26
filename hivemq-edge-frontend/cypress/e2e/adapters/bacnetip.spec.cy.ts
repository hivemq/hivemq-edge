import { MOCK_PROTOCOL_BACNET_IP } from '@/__test-utils__/adapters/bacnetip.ts'
import { loginPage, adapterPage } from 'cypress/pages'

import { cy_interceptCoreE2E } from 'cypress/utils/intercept.utils.ts'

describe('BACnet/IP', () => {
  beforeEach(() => {
    cy_interceptCoreE2E()

    cy.intercept('/api/v1/management/protocol-adapters/types', { items: [MOCK_PROTOCOL_BACNET_IP] })

    // TODO[E2E] This doesn't work: JWT needs mocking
    loginPage.visit('/app/protocol-adapters/catalog/new/bacnetip')
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
