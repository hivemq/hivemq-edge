import { loginPage, adapterPage, rjsf } from 'cypress/pages'

import { MOCK_PROTOCOL_EIP } from '@/__test-utils__/adapters/eip.ts'
import { cy_interceptCoreE2E } from 'cypress/utils/intercept.utils.ts'
import { cy_identifierShouldBeValid, cy_identifierShouldBeVisible } from 'cypress/utils/common_fields.utils.ts'

describe('EIP adapter', () => {
  beforeEach(() => {
    cy_interceptCoreE2E()

    // TODO[E2E] This is the mock for the EIP protocol adapter
    cy.intercept('/api/v1/management/protocol-adapters/types', { items: [MOCK_PROTOCOL_EIP] }).as('getProtocols')

    // TODO[E2E] This doesn't work: JWT needs mocking
    loginPage.visit('/app/#/protocol-adapters/catalog/new/eip')
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
      adapterPage.config.subTitle.should('contain.text', 'Ethernet IP Protocol Adapter')

      adapterPage.config.formTab(0).should('have.text', 'Connection')
      adapterPage.config.formTab(1).should('have.text', 'EIP to MQTT')
      adapterPage.config.formTab(2).should('have.text', 'EIP Device')

      adapterPage.config.formTabPanel.should('have.length', 3)
      adapterPage.config.formTabPanel.eq(0).should('not.have.attr', 'hidden')
      adapterPage.config.formTabPanel.eq(1).should('have.attr', 'hidden')
      adapterPage.config.formTabPanel.eq(2).should('have.attr', 'hidden')
    })

    it('should render the connection tab', () => {
      // Force initial validation
      adapterPage.config.submitButton.click()
      adapterPage.config.formTab(0).click()

      cy_identifierShouldBeVisible()

      rjsf.field('host').requiredLabel.should('contain.text', 'Host')
      rjsf.field('host').input.should('have.value', '')
      rjsf.field('host').errors.should('contain.text', "must have required property 'Host'")

      rjsf.field('port').requiredLabel.should('contain.text', 'Port')
      rjsf.field('port').inputUpDown.should('have.value', '44818')
      rjsf.field('port').helperText.should('contain.text', 'The port number on the device you wish to connect to')
    })

    it('should render the EIP To MQTT tab', () => {
      // Force initial validation
      adapterPage.config.submitButton.click()
      adapterPage.config.formTab(1).click()

      rjsf.field('eipToMqtt').title.should('contain.text', 'Ethernet IP To MQTT Config')
      rjsf
        .field('eipToMqtt')
        .description.should('contain.text', 'The configuration for a data stream from Ethernet IP to MQTT')

      rjsf.field(['eipToMqtt', 'maxPollingErrorsBeforeRemoval']).label.should('contain.text', 'Max. Polling Errors')
      rjsf.field(['eipToMqtt', 'maxPollingErrorsBeforeRemoval']).input.should('have.value', '10')
      rjsf
        .field(['eipToMqtt', 'maxPollingErrorsBeforeRemoval'])
        .helperText.should(
          'contain.text',
          'Max. errors polling the endpoint before the polling daemon is stopped (-1 for unlimited retries)'
        )

      rjsf.field(['eipToMqtt', 'pollingIntervalMillis']).label.should('contain.text', 'Polling Interval [ms]')
      rjsf.field(['eipToMqtt', 'pollingIntervalMillis']).input.should('have.value', '1000')
      rjsf
        .field(['eipToMqtt', 'pollingIntervalMillis'])
        .helperText.should('contain.text', 'Time in millisecond that this endpoint will be polled')
    })

    it('should render the EIP Device tab', () => {
      // Force initial validation
      adapterPage.config.formTab(2).click()
      adapterPage.config.submitButton.click()

      rjsf.field('backplane').label.should('contain.text', 'Backplane')
      rjsf.field('backplane').input.should('have.value', '1')
      rjsf.field('backplane').helperText.should('contain.text', 'Backplane device value')

      rjsf.field(['slot']).label.should('contain.text', 'Slot')
      rjsf.field(['slot']).input.should('have.value', '0')
      rjsf.field(['slot']).helperText.should('contain.text', 'Slot device value')
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
