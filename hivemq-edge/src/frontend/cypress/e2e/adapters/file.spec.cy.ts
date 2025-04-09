import { loginPage, adapterPage, rjsf } from 'cypress/pages'

import { MOCK_PROTOCOL_FILE } from '@/__test-utils__/adapters/file.ts'
import { cy_interceptCoreE2E } from 'cypress/utils/intercept.utils.ts'
import { cy_identifierShouldBeValid, cy_identifierShouldBeVisible } from 'cypress/utils/common_fields.utils.ts'

describe('File adapter', () => {
  beforeEach(() => {
    cy_interceptCoreE2E()

    // TODO[E2E] This is the mock for the File protocol adapter
    cy.intercept('/api/v1/management/protocol-adapters/types', { items: [MOCK_PROTOCOL_FILE] }).as('getProtocols')

    // TODO[E2E] This doesn't work: JWT needs mocking
    loginPage.visit('/app/protocol-adapters/catalog/new/file')
    loginPage.loginButton.click()
    adapterPage.navLink.click()
  })

  describe('Configuration', () => {
    beforeEach(() => {
      adapterPage.addNewAdapter.should('have.text', 'Add a new adapter').click()
      cy.getByTestId('protocol-create-adapter').click()
    })

    it('should render the config with tabs', () => {
      adapterPage.config.title.should('contain.text', 'Create a new adapter')
      adapterPage.config.subTitle.should('contain.text', 'File Adapter')

      adapterPage.config.formTab(0).should('have.text', 'Connection')
      adapterPage.config.formTab(1).should('have.text', 'File to MQTT')

      adapterPage.config.formTabPanel.should('have.length', 2)
      adapterPage.config.formTabPanel.eq(0).should('not.have.attr', 'hidden')
      adapterPage.config.formTabPanel.eq(1).should('have.attr', 'hidden')
    })

    it('should render the connection tab', () => {
      // Force initial validation
      adapterPage.config.submitButton.click()
      adapterPage.config.formTab(0).click()

      cy_identifierShouldBeVisible()
    })

    it('should render the File To MQTT tab', () => {
      // Force initial validation
      adapterPage.config.submitButton.click()
      adapterPage.config.formTab(1).click()

      rjsf.field('fileToMqtt').title.should('contain.text', 'File To MQTT Config')
      rjsf
        .field('fileToMqtt')
        .description.should('contain.text', 'The configuration for a data stream from File to MQTT')

      rjsf.field(['fileToMqtt', 'maxPollingErrorsBeforeRemoval']).label.should('contain.text', 'Max. Polling Errors')
      rjsf.field(['fileToMqtt', 'maxPollingErrorsBeforeRemoval']).input.should('have.value', '10')
      rjsf
        .field(['fileToMqtt', 'maxPollingErrorsBeforeRemoval'])
        .helperText.should(
          'contain.text',
          'Max. errors polling the endpoint before the polling daemon is stopped (-1 for unlimited retries)'
        )

      rjsf.field(['fileToMqtt', 'maxPollingErrorsBeforeRemoval']).label.should('contain.text', 'Max. Polling Errors')
      rjsf.field(['fileToMqtt', 'maxPollingErrorsBeforeRemoval']).input.should('have.value', '10')
      rjsf
        .field(['fileToMqtt', 'maxPollingErrorsBeforeRemoval'])
        .helperText.should(
          'contain.text',
          'Max. errors polling the endpoint before the polling daemon is stopped (-1 for unlimited retries)'
        )
    })

    it('should be accessible', () => {
      cy.injectAxe()
      adapterPage.config.submitButton.click()
      cy.checkAccessibility()
    })
  })

  describe('Validation', () => {
    beforeEach(() => {
      adapterPage.addNewAdapter.click()
      cy.getByTestId('protocol-create-adapter').click()
    })

    it('should handle error', () => {
      adapterPage.config.submitButton.click()
      cy_identifierShouldBeValid()
    })
  })
})
