import { loginPage, adapterPage, rjsf } from 'cypress/pages'

import { MOCK_PROTOCOL_ADS } from '@/__test-utils__/adapters/ads.ts'
import { cy_interceptCoreE2E } from 'cypress/utils/intercept.utils.ts'
import { cy_identifierShouldBeValid, cy_identifierShouldBeVisible } from 'cypress/utils/common_fields.utils.ts'

describe('ADS adapter', () => {
  beforeEach(() => {
    cy_interceptCoreE2E()

    // TODO[E2E] This is the mock for the EIP protocol adapter
    cy.intercept('/api/v1/management/protocol-adapters/types', { items: [MOCK_PROTOCOL_ADS] }).as('getProtocols')

    // TODO[E2E] This doesn't work: JWT needs mocking
    loginPage.visit('/app/protocol-adapters/catalog/new/ads')
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
      adapterPage.config.subTitle.should('contain.text', 'ADS Protocol Adapter')

      adapterPage.config.formTab(0).should('have.text', 'Connection')
      adapterPage.config.formTab(1).should('have.text', 'ADS To MQTT')
      adapterPage.config.formTab(2).should('have.text', 'ADS Device')

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
      rjsf.field('port').inputUpDown.should('have.value', '48898')
      rjsf.field('port').helperText.should('contain.text', 'The port number on the device to connect to')
    })

    it('should render the ADS To MQTT tab', () => {
      // Force initial validation
      adapterPage.config.submitButton.click()
      adapterPage.config.formTab(1).click()

      rjsf.field('adsToMqtt').title.should('contain.text', 'ADS To MQTT Config')
      rjsf.field('adsToMqtt').description.should('contain.text', 'The configuration for a data stream from ADS to MQTT')

      rjsf.field(['adsToMqtt', 'maxPollingErrorsBeforeRemoval']).label.should('contain.text', 'Max. Polling Errors')
      rjsf.field(['adsToMqtt', 'maxPollingErrorsBeforeRemoval']).input.should('have.value', '10')
      rjsf
        .field(['adsToMqtt', 'maxPollingErrorsBeforeRemoval'])
        .helperText.should(
          'contain.text',
          'Max. errors polling the endpoint before the polling daemon is stopped (-1 for unlimited retries)'
        )

      rjsf.field(['adsToMqtt', 'pollingIntervalMillis']).label.should('contain.text', 'Polling Interval [ms]')
      rjsf.field(['adsToMqtt', 'pollingIntervalMillis']).input.should('have.value', '1000')
      rjsf
        .field(['adsToMqtt', 'pollingIntervalMillis'])
        .helperText.should('contain.text', 'Time in millisecond that this endpoint will be polled')

      rjsf.field(['adsToMqtt', 'publishChangedDataOnly']).checkBox.should('have.attr', 'data-checked')

      rjsf
        .field(['adsToMqtt', 'publishChangedDataOnly'])
        .checkBoxLabel.should('have.text', 'Only publish data items that have changed since last poll')
    })

    it('should render the ADS Device tab', () => {
      // Force initial validation
      adapterPage.config.formTab(2).click()
      adapterPage.config.submitButton.click()

      rjsf.field('sourceAmsNetId').requiredLabel.should('contain.text', 'Source Ams Net Id')
      rjsf.field('sourceAmsNetId').input.should('have.value', '')
      rjsf.field('sourceAmsNetId').errors.should('contain.text', "must have required property 'Source Ams Net Id'")

      rjsf.field(['sourceAmsPort']).requiredLabel.should('contain.text', 'Source AMS Port')
      rjsf.field(['sourceAmsPort']).input.should('have.value', '48898')
      rjsf.field(['sourceAmsPort']).helperText.should('contain.text', 'The local AMS port number used by HiveMQ Edge')

      rjsf.field('targetAmsNetId').requiredLabel.should('contain.text', 'Target Ams Net Id')
      rjsf.field('targetAmsNetId').input.should('have.value', '')
      rjsf.field('targetAmsNetId').errors.should('contain.text', "must have required property 'Target Ams Net Id'")

      rjsf.field(['targetAmsPort']).requiredLabel.should('contain.text', 'Target AMS Port')
      rjsf.field(['targetAmsPort']).input.should('have.value', '851')
      rjsf.field(['targetAmsPort']).helperText.should('contain.text', 'The AMS port number on the device to connect to')
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
