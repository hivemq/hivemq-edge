import { MOCK_PROTOCOL_OPC_UA } from '@/__test-utils__/adapters/opc-ua.ts'

import { loginPage, adapterPage, rjsf } from 'cypress/pages'
import { cy_interceptCoreE2E } from 'cypress/utils/intercept.utils.ts'

describe('OPCUA adapter', () => {
  beforeEach(() => {
    cy_interceptCoreE2E()

    // TODO[E2E] This is the mock for the OPCUA protocol adapter
    cy.intercept('/api/v1/management/protocol-adapters/types', { items: [MOCK_PROTOCOL_OPC_UA] }).as('getProtocols')

    // TODO[E2E] This doesn't work: JWT needs mocking
    loginPage.visit('/app/protocol-adapters/catalog/new/opcua')
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
      adapterPage.config.subTitle.should('contain.text', 'OPC UA Protocol Adapter')

      adapterPage.config.formTab(0).should('have.text', 'Connection')
      adapterPage.config.formTab(1).should('have.text', 'OPC UA to MQTT')

      adapterPage.config.formTabPanel.should('have.length', 2)
      adapterPage.config.formTabPanel.eq(0).should('not.have.attr', 'hidden')
      adapterPage.config.formTabPanel.eq(1).should('have.attr', 'hidden')
    })

    it('should render the connection tab', () => {
      // Force initial validation
      adapterPage.config.submitButton.click()
      adapterPage.config.formTab(0).click()

      rjsf.field('id').requiredLabel.should('contain.text', 'Identifier')
      rjsf.field('id').input.should('have.attr', 'name', 'root_id')
      rjsf.field('id').errors.should('have.text', "must have required property 'Identifier'")

      rjsf.field('uri').requiredLabel.should('contain.text', 'OPC UA Server URI')
      rjsf.field('uri').input.should('have.attr', 'name', 'root_uri')
      rjsf.field('uri').errors.should('have.text', "must have required property 'OPC UA Server URI'")

      rjsf.field('overrideUri').checkBox.should('not.have.attr', 'data-checked')
      rjsf.field('overrideUri').checkBoxLabel.should('have.text', 'Override server returned endpoint URI')
      rjsf
        .field('overrideUri')
        .description.should(
          'have.text',
          'Overrides the endpoint URI returned from the OPC UA server with the hostname and port from the specified URI.'
        )

      rjsf.field('security').title.should('contain.text', 'security')

      rjsf.field(['security', 'policy']).label.should('contain.text', 'OPC UA security policy')
      rjsf.field(['security', 'policy']).select.should('have.text', 'NONE')
      rjsf
        .field(['security', 'policy'])
        .helperText.should('have.text', 'Security policy to use for communication with the server.')

      rjsf.field('tls').title.should('contain.text', 'tls')

      rjsf.field(['tls', 'enabled']).checkBox.should('not.have.attr', 'data-checked')
      rjsf.field(['tls', 'enabled']).checkBoxLabel.should('have.text', 'Enable TLS')
      rjsf.field(['tls', 'enabled']).description.should('have.text', 'Enables TLS encrypted connection')

      rjsf.field(['tls', 'keystore']).title.should('contain.text', 'Keystore')
      rjsf
        .field(['tls', 'keystore'])
        .description.should(
          'contain.text',
          'Keystore that contains the client certificate including the chain. Required for X509 authentication.'
        )
    })

    it('should render the OPCUA To MQTT tab', () => {
      // Force initial validation
      adapterPage.config.submitButton.click()
      adapterPage.config.formTab(1).click()

      rjsf.field('opcuaToMqtt').title.should('contain.text', 'OPC UA To MQTT Config')
      rjsf
        .field('opcuaToMqtt')
        .description.should('contain.text', 'The configuration for a data stream from OPC UA to MQTT')

      rjsf.field(['opcuaToMqtt', 'publishingInterval']).label.should('contain.text', 'OPC UA publishing interval [ms]')
      rjsf.field(['opcuaToMqtt', 'publishingInterval']).input.should('have.value', '1000')
      rjsf
        .field(['opcuaToMqtt', 'publishingInterval'])
        .helperText.should(
          'contain.text',
          'OPC UA publishing interval in milliseconds for this subscription on the server'
        )
    })

    it('should be accessible', () => {
      cy.injectAxe()
      adapterPage.config.submitButton.click()
      cy.checkAccessibility()
    })
  })

  // describe.skip('Validation', () => {
  //   beforeEach(() => {
  //     adapterPage.addNewAdapter.click()
  //     cy.getByTestId('protocol-create-adapter').click()
  //   })
  //
  //   it('should handle error', () => {
  //     adapterPage.config.submitButton.click()
  //
  //     adapterPage.config.errorSummary.should('have.length', 2)
  //     adapterPage.config.errorSummaryFocus(0).click()
  //     // eslint-disable-next-line cypress/no-unnecessary-waiting
  //     cy.wait(50)
  //     cy.focused().type('123')
  //
  //     adapterPage.config.errorSummary.should('have.length', 1)
  //
  //     adapterPage.config.formTabPanel.eq(0).within(() => {
  //       adapterPage.config.formField.eq(1).within(() => {
  //         rjsFormField.input.should('have.attr', 'name', 'root_host').should('have.value', '123')
  //       })
  //     })
  //
  //     adapterPage.config.errorSummaryFocus(0).click()
  //     // eslint-disable-next-line cypress/no-unnecessary-waiting
  //     cy.wait(50)
  //     cy.focused().type('123')
  //
  //     adapterPage.config.errorSummary.should('not.exist')
  //
  //     adapterPage.config.submitButton.click()
  //     adapterPage.config.panel.should('not.exist')
  //   })
  //
  //   it('should handle identifier', () => {
  //     adapterPage.config.submitButton.click()
  //
  //     adapterPage.config.errorSummaryFocus(1).click()
  //     // eslint-disable-next-line cypress/no-unnecessary-waiting
  //     cy.wait(50)
  //     cy.focused().type('opcua-1')
  //
  //     adapterPage.config.formTabPanel.eq(0).within(() => {
  //       adapterPage.config.formField.eq(0).within(() => {
  //         rjsFormField.input.should('have.value', 'opcua-1')
  //         rjsFormField.errors.should('have.text', 'This identifier is already in use for another adapter')
  //       })
  //     })
  //   })
  // })
  //
  // describe.skip('Workspace', () => {
  //   beforeEach(() => {
  //     const mockResponse: DomainTagList = { items: MOCK_DEVICE_TAGS('s7-1', MockAdapterType.SIMULATION) }
  //
  //     cy.intercept('/api/v1/management/protocol-adapters/adapters/*/tags', mockResponse).as('tags')
  //     cy.intercept('/api/v1/management/protocol-adapters/tag-schemas/s7', MOCK_SCHEMA_S7)
  //
  //     cy.intercept('/api/v1/management/protocol-adapters/adapters', { items: [MOCK_ADAPTER_S7] }).as('getAdapters')
  //   })
  //
  //   it.only('should render the nodes', () => {
  //     cy.wait('@getAdapters')
  //     // cy.wait('@tags')
  //
  //     workspacePage.navLink.click()
  //     workspacePage.canvas.should('be.visible')
  //     workspacePage.toolbox.fit.click().type('tab')
  //
  //     workspacePage.edgeNode.click()
  //     workspacePage.nodeToolbar.should('be.visible').should('have.attr', 'data-id', 'edge')
  //
  //     workspacePage.adapterNode('s7-1').click()
  //     workspacePage.toolbar.title.should('have.text', 's7-1')
  //     workspacePage.deviceNode('s7-1').click()
  //     workspacePage.toolbar.title.should('have.text', 'S7')
  //
  //     // TODO multiple selection is not working
  //     // workspacePage.adapterNode('s7-1').click()
  //     // workspacePage.deviceNode('s7-1').click({ metaKey: true })
  //     // workspacePage.nodeToolbar.should('be.visible')
  //     // workspacePage.toolbar.title.should('have.text', '1 entities selected')
  //   })
  // })
})
