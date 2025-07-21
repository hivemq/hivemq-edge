import { drop, factory, primaryKey } from '@mswjs/data'

import type { Bridge } from '@/api/__generated__'
import { cy_interceptCoreE2E } from 'cypress/utils/intercept.utils.ts'
import { bridgePage, loginPage, rjsf } from 'cypress/pages'

describe('Bridges', () => {
  // Creating a mock storage for the Bridges
  const mswDB = factory({
    bridge: {
      id: primaryKey(String),
      json: String,
    },
  })

  beforeEach(() => {
    drop(mswDB)

    cy_interceptCoreE2E()

    // TODO[NVL] Add support for the Event Log
    cy.intercept('GET', '/api/v1/management/bridges', (req) => {
      const allBridgeData = mswDB.bridge.getAll()
      const allBridges = allBridgeData.map<Bridge>((data) => ({ ...JSON.parse(data.json) }))
      req.reply(200, { items: allBridges })
    })

    cy.intercept<Bridge>('POST', '/api/v1/management/bridges', (req) => {
      const bridge = req.body
      const newBridgeData = mswDB.bridge.create({
        id: bridge.id,
        json: JSON.stringify(bridge),
      })
      req.reply(200, newBridgeData)
    })

    cy.intercept<Bridge>('PUT', '/api/v1/management/bridges/**', (req) => {
      const bridge = req.body

      mswDB.bridge.update({
        where: {
          id: {
            equals: bridge.id,
          },
        },

        data: { json: JSON.stringify(bridge) },
      })

      req.reply(200, '')
    })

    cy.intercept<Bridge>('DELETE', '/api/v1/management/bridges/**', (req) => {
      const urlParts = req.url.split('/')
      const bridgeId = urlParts[urlParts.length - 1]

      mswDB.bridge.delete({
        where: {
          id: {
            equals: bridgeId,
          },
        },
      })
      req.reply(200, '')
    }).as('deleteBridge')

    loginPage.visit('/app/mqtt-bridges')
    loginPage.loginButton.click()
    bridgePage.navLink.click()
  })

  context('Bridge configuration', () => {
    // TODO[NVL] Add support for the toasts

    it('should render the landing page', () => {
      cy.location().should((loc) => {
        expect(loc.pathname).to.eq('/app/mqtt-bridges')
      })

      bridgePage.pageHeader.should('have.text', 'MQTT bridges')
      bridgePage.pageHeaderSubTitle.should(
        'have.text',
        'MQTT bridges let you connect multiple MQTT brokers to enable seamless data sharing between different networks or systems.'
      )
      bridgePage.addNewBridge.should('have.text', 'Add bridge connection')

      bridgePage.table.container.should('be.visible')
      bridgePage.table.status
        .should('have.attr', 'data-status', 'info')
        .should('have.text', 'No bridges currently created')
    })

    it('should create a new bridge', () => {
      cy.location().should((loc) => {
        expect(loc.pathname).to.eq('/app/mqtt-bridges')
      })

      bridgePage.table.status
        .should('have.attr', 'data-status', 'info')
        .should('have.text', 'No bridges currently created')

      bridgePage.addNewBridge.click()

      cy.location().should((loc) => {
        expect(loc.pathname).to.eq('/app/mqtt-bridges/new')
      })

      bridgePage.config.panel.should('be.visible')
      bridgePage.config.title.should('have.text', 'Create a new bridge configuration')
      bridgePage.config.submitButton.should('have.text', 'Create the bridge')

      bridgePage.config.errorSummary.should('have.length', 3)
      bridgePage.config.errorSummaryFocus(0).should('exist')

      rjsf.field('id').input.should('not.have.value')
      rjsf.field('id').input.type('my-bridge')
      rjsf.field('host').input.type('my-host')
      rjsf.field('clientId').input.type('my-client-id')
      bridgePage.config.errorSummary.should('have.length', 0)

      bridgePage.config.submitButton.click()

      cy.location().should((loc) => {
        expect(loc.pathname).to.eq('/app/mqtt-bridges')
      })

      bridgePage.table.rows.should('have.length', 1)
      bridgePage.table.cell(0, 'id').should('have.text', 'my-bridge')

      bridgePage.table.action(0, 'edit').click()

      rjsf.field('id').input.should('be.disabled')
      rjsf.field('port').input.clear().type('2999')

      bridgePage.config.submitButton.click()

      //TODO[NVL] Better create a subscription
    })

    it('should create a new bridge and delete it', () => {
      cy.location().should((loc) => {
        expect(loc.pathname).to.eq('/app/mqtt-bridges')
      })

      bridgePage.table.status
        .should('have.attr', 'data-status', 'info')
        .should('have.text', 'No bridges currently created')

      bridgePage.addNewBridge.click()

      bridgePage.config.panel.should('be.visible')
      bridgePage.config.submitButton.should('have.text', 'Create the bridge')

      rjsf.field('id').input.type('my-bridge')
      rjsf.field('host').input.type('my-host')
      rjsf.field('clientId').input.type('my-client-id')

      bridgePage.config.submitButton.click()

      bridgePage.table.action(0, 'delete').click()
      bridgePage.modal.dialog.should('be.visible')
      bridgePage.modal.title.should('have.text', 'Delete Bridge')
      bridgePage.modal.prompt.should('have.text', "Are you sure? You can't undo this action afterward.")
      bridgePage.modal.confirm.should('have.text', 'Delete')
      bridgePage.modal.confirm.click()

      cy.wait('@deleteBridge')
      bridgePage.table.status
        .should('have.attr', 'data-status', 'info')
        .should('have.text', 'No bridges currently created')
    })
  })

  context('Bridge in Workspace', () => {
    it('should create a bridge also in the Workspace', () => {
      bridgePage.table.status.should('have.text', 'No bridges currently created')

      bridgePage.addNewBridge.click()

      bridgePage.config.panel.should('be.visible')
      bridgePage.config.title.should('have.text', 'Create a new bridge configuration')
      bridgePage.config.submitButton.should('have.text', 'Create the bridge')

      rjsf.field('id').input.type('my-bridge')
      rjsf.field('host').input.type('my-host')
      rjsf.field('clientId').input.type('my-client-id')

      bridgePage.config.submitButton.click()

      //TODO[NVL] Better create a subscription
    })
  })

  context('Bridge in Event Log', () => {})

  it.only('should be accessible', () => {
    cy.injectAxe()
    bridgePage.table.status.should('have.text', 'No bridges currently created')

    bridgePage.addNewBridge.click()

    bridgePage.config.panel.should('be.visible')
    bridgePage.config.title.should('have.text', 'Create a new bridge configuration')
    bridgePage.config.submitButton.should('have.text', 'Create the bridge')

    rjsf.field('id').input.type('my-bridge')
    rjsf.field('host').input.type('my-host')
    rjsf.field('clientId').input.type('my-client-id')

    bridgePage.config.submitButton.click()

    bridgePage.toast.close()

    bridgePage.table.action(0, 'edit').click()

    bridgePage.config.formTab(4).click() // Click on the "Subscriptions" tab

    cy.checkAccessibility()
  })
})
