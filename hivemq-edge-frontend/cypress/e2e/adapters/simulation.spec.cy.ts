import { loginPage, adapterPage, rjsf } from 'cypress/pages'

import { MOCK_PROTOCOL_SIMULATION } from '@/__test-utils__/adapters/simulation.ts'
import { cy_interceptCoreE2E } from 'cypress/utils/intercept.utils.ts'
import { cy_identifierShouldBeValid, cy_identifierShouldBeVisible } from 'cypress/utils/common_fields.utils.ts'

describe('Simulation adapter', () => {
  beforeEach(() => {
    cy_interceptCoreE2E()

    // TODO[E2E] This is the mock for the Simulation protocol adapter
    cy.intercept('/api/v1/management/protocol-adapters/types', { items: [MOCK_PROTOCOL_SIMULATION] }).as('getProtocols')

    // TODO[E2E] This doesn't work: JWT needs mocking
    loginPage.visit('/app/#/protocol-adapters/catalog/new/simulation')
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
      adapterPage.config.subTitle.should('contain.text', 'Simulated Edge Device')

      adapterPage.config.formTab(0).should('have.text', 'Settings')
      adapterPage.config.formTab(1).should('have.text', 'Simulation to MQTT')

      adapterPage.config.formTabPanel.should('have.length', 2)
      adapterPage.config.formTabPanel.eq(0).should('not.have.attr', 'hidden')
      adapterPage.config.formTabPanel.eq(1).should('have.attr', 'hidden')
    })

    it('should render the settings tab', () => {
      // Force initial validation
      adapterPage.config.submitButton.click()
      adapterPage.config.formTab(0).click()

      cy_identifierShouldBeVisible()

      rjsf.field('minValue').label.should('contain.text', 'Min. Generated Value')
      rjsf.field('minValue').input.should('have.value', '0')
      rjsf.field('minValue').helperText.should('contain.text', 'Minimum value of the generated decimal number')

      rjsf.field('maxValue').label.should('contain.text', 'Max. Generated Value (Excl.)')
      rjsf.field('maxValue').input.should('have.value', '1000')
      rjsf
        .field('maxValue')
        .helperText.should('contain.text', 'Maximum value of the generated decimal number (excluded)')

      rjsf.field('minDelay').label.should('contain.text', 'Minimum of delay')
      rjsf.field('minDelay').input.should('have.value', '0')
      rjsf
        .field('minDelay')
        .helperText.should('contain.text', 'Minimum of artificial delay before the polling method generates a value')

      rjsf.field('maxDelay').label.should('contain.text', 'Maximum of delay')
      rjsf.field('maxDelay').input.should('have.value', '0')
      rjsf
        .field('maxDelay')
        .helperText.should('contain.text', 'Maximum of artificial delay before the polling method generates a value')
    })

    it('should render the Simulation To MQTT tab', () => {
      // Force initial validation
      adapterPage.config.submitButton.click()
      adapterPage.config.formTab(1).click()

      rjsf.field('simulationToMqtt').title.should('contain.text', 'simulationToMqtt')
      rjsf.field('simulationToMqtt').description.should('contain.text', 'Define Simulations to create MQTT messages.')

      rjsf.field(['simulationToMqtt', 'pollingIntervalMillis']).label.should('contain.text', 'Polling Interval [ms]')
      rjsf.field(['simulationToMqtt', 'pollingIntervalMillis']).input.should('have.value', '1000')
      rjsf
        .field(['simulationToMqtt', 'pollingIntervalMillis'])
        .helperText.should('contain.text', 'Time in millisecond that this endpoint will be polled')

      rjsf
        .field(['simulationToMqtt', 'maxPollingErrorsBeforeRemoval'])
        .label.should('contain.text', 'Max. Polling Errors')
      rjsf.field(['simulationToMqtt', 'maxPollingErrorsBeforeRemoval']).input.should('have.value', '10')
      rjsf
        .field(['simulationToMqtt', 'maxPollingErrorsBeforeRemoval'])
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
