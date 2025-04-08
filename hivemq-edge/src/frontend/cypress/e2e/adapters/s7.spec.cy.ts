import { MockAdapterType } from '@/__test-utils__/adapters/types.ts'
import { MOCK_ADAPTER_S7, MOCK_PROTOCOL_S7, MOCK_SCHEMA_S7 } from '@/__test-utils__/adapters/s7.ts'

import type { DomainTagList } from '@/api/__generated__'
import { MOCK_DEVICE_TAGS } from '@/api/hooks/useProtocolAdapters/__handlers__'

import { loginPage, adapterPage, rjsf, workspacePage } from 'cypress/pages'
import { cy_interceptCoreE2E } from 'cypress/utils/intercept.utils.ts'

describe('S7 adapter', () => {
  beforeEach(() => {
    cy_interceptCoreE2E()

    // TODO[E2E] This is the mock for the S7 protocol adapter
    cy.intercept('/api/v1/management/protocol-adapters/types', { items: [MOCK_PROTOCOL_S7] }).as('getProtocols')

    // TODO[E2E] This doesn't work: JWT needs mocking
    loginPage.visit('/app/protocol-adapters/catalog/new/s7')
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
      adapterPage.config.subTitle.should('contain.text', 'S7 Protocol Adapter')

      adapterPage.config.formTab(0).should('have.text', 'Connection')
      adapterPage.config.formTab(1).should('have.text', 'S7 To MQTT')
      adapterPage.config.formTab(2).should('have.text', 'S7 Device')

      adapterPage.config.formTabPanel.should('have.length', 3)
      adapterPage.config.formTabPanel.eq(0).should('not.have.attr', 'hidden')
      adapterPage.config.formTabPanel.eq(1).should('have.attr', 'hidden')
      adapterPage.config.formTabPanel.eq(2).should('have.attr', 'hidden')
    })

    it('should render the connection tab', () => {
      // Force initial validation
      adapterPage.config.submitButton.click()
      adapterPage.config.formTab(0).click()

      adapterPage.config.formTabPanel.eq(0).within(() => {
        adapterPage.config.formField.should('have.length', 3)

        adapterPage.config.formField.eq(0).within(() => {
          rjsf.requiredLabel.should('contain.text', 'Identifier')
          rjsf.input.should('have.attr', 'name', 'root_id')
          rjsf.errors.should('have.text', "must have required property 'Identifier'")
        })
        adapterPage.config.formField.eq(1).within(() => {
          rjsf.requiredLabel.should('contain.text', 'Host')
          rjsf.input.should('have.attr', 'name', 'root_host')
          rjsf.errors.should('have.text', "must have required property 'Host'")
        })

        adapterPage.config.formField.eq(2).within(() => {
          rjsf.requiredLabel.should('contain.text', 'Port')
          rjsf.inputUpDown.should('have.attr', 'name', 'root_port').should('have.value', 102)
        })
      })
    })

    it('should render the S7 To MQTT tab', () => {
      // Force initial validation
      adapterPage.config.submitButton.click()
      adapterPage.config.formTab(1).click()

      adapterPage.config.formTabPanel.eq(0).should('have.attr', 'hidden')
      adapterPage.config.formTabPanel.eq(1).should('not.have.attr', 'hidden')
      adapterPage.config.formTabPanel.eq(2).should('have.attr', 'hidden')

      adapterPage.config.formTabPanel.eq(1).within(() => {
        adapterPage.config.formField.should('have.length', 4)

        adapterPage.config.formField.eq(0).within(() => {
          rjsf.title.should('have.text', 'S7 To MQTT Config')
          rjsf.subtitle.should('have.text', 'The configuration for a data stream from S7 to MQTT')
        })

        adapterPage.config.formField.eq(1).within(() => {
          rjsf.label.should('have.text', 'Max. Polling Errors')
          rjsf.input.should('have.attr', 'name', 'root_s7ToMqtt_maxPollingErrorsBeforeRemoval').should('have.value', 10)
          rjsf.helperText.should(
            'have.text',
            'Max. errors polling the endpoint before the polling daemon is stopped (-1 for unlimited retries)'
          )
        })

        adapterPage.config.formField.eq(2).within(() => {
          rjsf.label.should('have.text', 'Polling Interval [ms]')
          rjsf.input.should('have.value', 1000)
          rjsf.helperText.should('have.text', 'Time in millisecond that this endpoint will be polled')
        })

        adapterPage.config.formField.eq(3).within(() => {
          rjsf.label.should('have.text', 'Only publish data items that have changed since last poll')
          rjsf.checkBox.should('have.attr', 'data-checked')
        })
      })
    })

    it('should render the S7 Device tab', () => {
      // Force initial validation
      adapterPage.config.submitButton.click()
      adapterPage.config.formTab(2).click()

      adapterPage.config.formTabPanel.eq(0).should('have.attr', 'hidden')
      adapterPage.config.formTabPanel.eq(1).should('have.attr', 'hidden')
      adapterPage.config.formTabPanel.eq(2).should('not.have.attr', 'hidden')

      adapterPage.config.formTabPanel.eq(2).within(() => {
        adapterPage.config.formField.should('have.length', 6)

        adapterPage.config.formField.eq(0).within(() => {
          rjsf.requiredLabel.should('contain.text', 'S7 Controller Type')
          rjsf.select.should('have.text', 'S7_300')
          rjsf.helperText.should('have.text', 'The type of the S7 Controller')
          // rjsFormField.select.click().type('LOGO')
        })

        adapterPage.config.formField.eq(1).within(() => {
          rjsf.label.should('have.text', 'Remote Rack')
          rjsf.input.should('have.value', 0)
          rjsf.helperText.should('have.text', 'Rack value for the remote main CPU (PLC).')
        })

        adapterPage.config.formField.eq(2).within(() => {
          rjsf.label.should('have.text', 'Remote Rack 2')
          rjsf.input.should('have.value', 0)
          rjsf.helperText.should('have.text', 'Rack value for the remote secondary CPU (PLC).')
        })

        adapterPage.config.formField.eq(3).within(() => {
          rjsf.label.should('have.text', 'Remote Slot')
          rjsf.input.should('have.value', 0)
          rjsf.helperText.should('have.text', 'Slot value for the remote main CPU (PLC).')
        })

        adapterPage.config.formField.eq(4).within(() => {
          rjsf.label.should('have.text', 'Remote Slot 2')
          rjsf.input.should('have.value', 0)
          rjsf.helperText.should('have.text', 'Slot value for the remote secondary CPU (PLC).')
        })

        adapterPage.config.formField.eq(5).within(() => {
          rjsf.label.should('have.text', 'Remote TSAP')
          rjsf.input.should('have.value', 0)
          rjsf.helperText.should(
            'have.text',
            'Remote TSAP value. The TSAP (Transport Services Access Point) mechanism is used as a further addressing level in the S7 PLC network. Usually only required for PLC from the LOGO series.'
          )
        })
      })
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

      adapterPage.config.errorSummary.should('have.length', 2)
      adapterPage.config.errorSummaryFocus(0).click()
      // eslint-disable-next-line cypress/no-unnecessary-waiting
      cy.wait(50)
      cy.focused().type('123')

      adapterPage.config.errorSummary.should('have.length', 1)

      adapterPage.config.formTabPanel.eq(0).within(() => {
        adapterPage.config.formField.eq(1).within(() => {
          rjsf.input.should('have.attr', 'name', 'root_host').should('have.value', '123')
        })
      })

      adapterPage.config.errorSummaryFocus(0).click()
      // eslint-disable-next-line cypress/no-unnecessary-waiting
      cy.wait(50)
      cy.focused().type('123')

      adapterPage.config.errorSummary.should('not.exist')

      adapterPage.config.submitButton.click()
      adapterPage.config.panel.should('not.exist')
    })

    it('should handle identifier', () => {
      adapterPage.config.submitButton.click()

      adapterPage.config.errorSummaryFocus(1).click()
      // eslint-disable-next-line cypress/no-unnecessary-waiting
      cy.wait(50)
      cy.focused().type('opcua-1')

      adapterPage.config.formTabPanel.eq(0).within(() => {
        adapterPage.config.formField.eq(0).within(() => {
          rjsf.input.should('have.value', 'opcua-1')
          rjsf.errors.should('have.text', 'This identifier is already in use for another adapter')
        })
      })
    })
  })

  describe('Workspace', () => {
    beforeEach(() => {
      const mockResponse: DomainTagList = { items: MOCK_DEVICE_TAGS('s7-1', MockAdapterType.SIMULATION) }

      cy.intercept('/api/v1/management/protocol-adapters/adapters/*/tags', mockResponse).as('tags')
      cy.intercept('/api/v1/management/protocol-adapters/tag-schemas/s7', MOCK_SCHEMA_S7)

      cy.intercept('/api/v1/management/protocol-adapters/adapters', { items: [MOCK_ADAPTER_S7] }).as('getAdapters')
    })

    it('should render the nodes', () => {
      cy.wait('@getAdapters')
      // cy.wait('@tags')

      workspacePage.navLink.click()
      workspacePage.canvas.should('be.visible')
      workspacePage.toolbox.fit.click().type('tab')

      workspacePage.edgeNode.click()
      workspacePage.nodeToolbar.should('be.visible').should('have.attr', 'data-id', 'edge')

      workspacePage.adapterNode('s7-1').click()
      workspacePage.toolbar.title.should('have.text', 's7-1')
      workspacePage.deviceNode('s7-1').click()
      workspacePage.toolbar.title.should('have.text', 'S7')

      // TODO multiple selection is not working
      // workspacePage.adapterNode('s7-1').click()
      // workspacePage.deviceNode('s7-1').click({ metaKey: true })
      // workspacePage.nodeToolbar.should('be.visible')
      // workspacePage.toolbar.title.should('have.text', '1 entities selected')
    })
  })
})
