import {
  MOCK_PROTOCOL_ADS,
  MOCK_PROTOCOL_BACNET_IP,
  MOCK_PROTOCOL_DATABASES,
  MOCK_PROTOCOL_EIP,
  MOCK_PROTOCOL_FILE,
  MOCK_PROTOCOL_HTTP,
  MOCK_PROTOCOL_MODBUS,
  MOCK_PROTOCOL_MTCONNECT,
  MOCK_PROTOCOL_OPC_UA,
  MOCK_PROTOCOL_S7,
  MOCK_PROTOCOL_SIMULATION,
} from '@/__test-utils__/adapters'

import { loginPage, adapterPage } from 'cypress/pages'

import { cy_interceptCoreE2E } from 'cypress/utils/intercept.utils.ts'

describe.skip('Adapters', () => {
  // Not to be used yet for E2E testing
  beforeEach(() => {
    cy_interceptCoreE2E()

    cy.intercept('/api/v1/management/protocol-adapters/types', {
      items: [
        MOCK_PROTOCOL_ADS,
        MOCK_PROTOCOL_BACNET_IP,
        MOCK_PROTOCOL_DATABASES,
        MOCK_PROTOCOL_EIP,
        MOCK_PROTOCOL_FILE,
        MOCK_PROTOCOL_HTTP,
        MOCK_PROTOCOL_OPC_UA,
        MOCK_PROTOCOL_S7,
        MOCK_PROTOCOL_MODBUS,
        MOCK_PROTOCOL_MTCONNECT,
        MOCK_PROTOCOL_SIMULATION,
      ],
    }).as('getProtocols')

    loginPage.visit('/app/protocol-adapters')
    loginPage.loginButton.click()
    adapterPage.navLink.click()
  })

  it('should be accessible', { tags: ['@percy'] }, () => {
    cy.injectAxe()
    cy.checkAccessibility(undefined, {
      rules: {
        'color-contrast': { enabled: false },
      },
    })
    cy.percySnapshot('Page: Adapters')

    adapterPage.addNewAdapter.click()

    cy.checkAccessibility(undefined, {
      rules: {
        'color-contrast': { enabled: false },
      },
    })
    cy.percySnapshot('Page: Adapters / Protocols')
  })
})
