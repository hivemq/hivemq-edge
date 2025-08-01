import { drop, factory, primaryKey } from '@mswjs/data'

import { cy_interceptCoreE2E, cy_interceptWithMockDB } from 'cypress/utils/intercept.utils.ts'
import { adapterPage, loginPage, rjsf, eventLogPage, bridgePage } from 'cypress/pages'

/**
 * This E2E test suite is of limited value without a real backend, since the log only renders whatever has been
 * generated by other parts of Edge.
 */
describe('Event Log', () => {
  // Creating a mock storage
  const mswDB = factory({
    eventLog: {
      id: primaryKey(String),
      json: String,
    },
    bridge: {
      id: primaryKey(String),
      json: String,
    },
    adapter: {
      id: primaryKey(String),
      json: String,
    },
  })

  beforeEach(() => {
    drop(mswDB)

    cy_interceptCoreE2E()

    cy_interceptWithMockDB(mswDB)

    loginPage.visit('/app/event-logs')
    loginPage.loginButton.click()
    eventLogPage.navLink.click()
  })

  it('should render landing page and events', () => {
    cy.location().should((loc) => {
      expect(loc.pathname).to.eq('/app/event-logs')
    })

    // Add an adapter to generate events
    adapterPage.navLink.click()
    adapterPage.addNewAdapter.click()
    adapterPage.protocols.createNewConnection(0).click()
    rjsf.field('id').input.type('test-adapter1')
    rjsf.field('uri').input.type('opc.tcp://my-local-machine.local:53530/OPCUA/SimulationServer')
    adapterPage.config.submitButton.click()
    adapterPage.toast.close()

    eventLogPage.navLink.click()
    eventLogPage.table.rows.should('have.length', 1)
    eventLogPage.table.cell(0, 'severity').should('have.text', 'INFO')
    eventLogPage.table.cell(0, 'source').should('have.text', 'test-adapter1')

    // Add a bridge to generate events
    bridgePage.navLink.click()
    bridgePage.addNewBridge.click()
    rjsf.field('id').input.type('my-bridge')
    rjsf.field('host').input.type('my-host')
    rjsf.field('clientId').input.type('my-client-id')
    bridgePage.config.submitButton.click()
    adapterPage.toast.close()

    eventLogPage.navLink.click()
    eventLogPage.updateEventLog.click()
    eventLogPage.table.rows.should('have.length', 2)
    eventLogPage.table.cell(1, 'severity').should('have.text', 'ERROR')
    eventLogPage.table.cell(1, 'source').should('have.text', 'my-bridge')
    eventLogPage.table.cell(1, 'details').find('button').click()

    eventLogPage.detailPanel.panel.should('be.visible')
    eventLogPage.detailPanel.title.should('have.text', 'Event')
    eventLogPage.detailPanel.eventType.should('have.text', 'ERROR')

    eventLogPage.detailPanel.closeButton.click()
  })

  it('should be accessible', () => {
    cy.injectAxe()

    cy.location().should((loc) => {
      expect(loc.pathname).to.eq('/app/event-logs')
    })

    cy.checkAccessibility()
  })
})
