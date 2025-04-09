import { MOCK_PROTOCOL_HTTP } from '@/__test-utils__/adapters/http.ts'
import { cy_interceptCoreE2E } from 'cypress/utils/intercept.utils'
import { cy_identifierShouldBeVisible } from 'cypress/utils/common_fields.utils.ts'
import { adapterPage, loginPage, rjsf } from 'cypress/pages'

describe('Http adapter', () => {
  beforeEach(() => {
    cy_interceptCoreE2E()

    // TODO[E2E] This is the mock for the HTTP protocol adapter
    cy.intercept('/api/v1/management/protocol-adapters/types', { items: [MOCK_PROTOCOL_HTTP] }).as('getProtocols')

    // TODO[E2E] This doesn't work: JWT needs mocking
    loginPage.visit('/app/protocol-adapters/catalog/new/http')
    loginPage.loginButton.click()
    adapterPage.navLink.click()
  })

  describe('Configuration', () => {
    beforeEach(() => {
      adapterPage.addNewAdapter.should('have.text', 'Add a new adapter').click()
      cy.getByTestId('protocol-create-adapter').click()
    })

    it('should render the config with tabs', () => {
      // cy.wait('@getProtocols')
      // cy.wait('@getAdapters')

      adapterPage.config.title.should('contain.text', 'Create a new adapter')
      adapterPage.config.subTitle.should('contain.text', 'HTTP(s) to MQTT Protocol Adapter')

      adapterPage.config.formTab(0).should('have.text', 'Connection')
      adapterPage.config.formTab(1).should('have.text', 'HTTP to MQTT')

      adapterPage.config.formTabPanel.should('have.length', 2)
      adapterPage.config.formTabPanel.eq(0).should('not.have.attr', 'hidden')
      adapterPage.config.formTabPanel.eq(1).should('have.attr', 'hidden')
    })

    it('should render the connection tab', () => {
      // Force initial validation
      adapterPage.config.submitButton.click()
      adapterPage.config.formTab(0).click()

      cy_identifierShouldBeVisible()

      rjsf.field('allowUntrustedCertificates').checkBox.should('not.have.attr', 'data-checked')
      rjsf.field('allowUntrustedCertificates').checkBoxLabel.should('have.text', 'Allow Untrusted Certificates')
      rjsf
        .field('allowUntrustedCertificates')
        .description.should(
          'have.text',
          'Allow the adapter to connect to untrusted SSL sources (for example expired certificates).'
        )

      rjsf.field('httpConnectTimeoutSeconds').label.should('contain.text', 'HTTP Connection Timeout')
      rjsf
        .field('httpConnectTimeoutSeconds')
        .input.should('have.attr', 'name', 'root_httpConnectTimeoutSeconds')
        .should('have.value', 5)
      rjsf
        .field('httpConnectTimeoutSeconds')
        .helperText.should(
          'have.text',
          'Timeout (in seconds) to allow the underlying HTTP connection to be established'
        )
    })

    it('should render the HTTP To MQTT tab', () => {
      // Force initial validation
      adapterPage.config.formTab(1).click()
      adapterPage.config.submitButton.click()

      rjsf.field('httpToMqtt').title.should('contain.text', 'HTTP To MQTT Config')
      rjsf
        .field('httpToMqtt')
        .description.should('contain.text', 'The configuration for a data stream from HTTP to MQTT')

      rjsf.field(['httpToMqtt', 'httpPublishSuccessStatusCodeOnly']).checkBox.should('have.attr', 'data-checked')
      rjsf
        .field(['httpToMqtt', 'httpPublishSuccessStatusCodeOnly'])
        .description.should('have.text', 'Only publish data when HTTP response code is successful ( 200 - 299 )')
      rjsf
        .field(['httpToMqtt', 'httpPublishSuccessStatusCodeOnly'])
        .checkBoxLabel.should('have.text', 'Publish Only On Success Codes')
    })

    it('should be accessible', () => {
      cy.injectAxe()
      adapterPage.config.submitButton.click()
      cy.checkAccessibility()
    })
  })
})
