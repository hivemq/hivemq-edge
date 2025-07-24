import { drop, factory, primaryKey } from '@mswjs/data'

import { cy_interceptCoreE2E, cy_interceptWithMockDB } from 'cypress/utils/intercept.utils.ts'
import { bridgePage, loginPage, rjsf, workspacePage } from 'cypress/pages'
import { workspaceBridgePanel } from '../../pages/Workspace/BridgeFormPage.ts'
import { MOCK_TOPIC_FILTER } from '@/api/hooks/useTopicFilters/__handlers__'

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

    cy_interceptWithMockDB(mswDB)

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
    beforeEach(() => {
      cy.intercept('/api/v1/management/protocol-adapters/types', { statusCode: 202, log: false })
      cy.intercept('/api/v1/gateway/listeners', { statusCode: 202, log: false })
      cy.intercept('/api/v1/management/combiners', { statusCode: 202, log: false })
      cy.intercept('/api/v1/management/protocol-adapters/adapters/**/northboundMappings', {
        statusCode: 202,
        log: false,
      })
      cy.intercept('/api/v1/management/protocol-adapters/adapters/**/southboundMappings', {
        statusCode: 202,
        log: false,
      })
      cy.intercept('/api/v1/management/protocol-adapters/adapters/**/tags', { statusCode: 202, log: false })
      cy.intercept('/api/v1/data-hub/data-validation/policies', { statusCode: 202, log: false })
      cy.intercept('/api/v1/management/topic-filters', {
        items: [MOCK_TOPIC_FILTER],
      })
    })

    it('should create a bridge also in the Workspace', () => {
      bridgePage.table.status.should('have.text', 'No bridges currently created')

      workspacePage.navLink.click()
      workspacePage.edgeNode.should('contain.text', 'HiveMQ Edge')
      workspacePage.bridgeNodes().should('have.length', 0)

      bridgePage.navLink.click()

      bridgePage.addNewBridge.click()

      bridgePage.config.panel.should('be.visible')
      bridgePage.config.title.should('have.text', 'Create a new bridge configuration')
      bridgePage.config.submitButton.should('have.text', 'Create the bridge')

      rjsf.field('id').input.type('my-bridge')
      rjsf.field('host').input.type('my-host')
      rjsf.field('clientId').input.type('my-client-id')

      bridgePage.config.submitButton.click()
      bridgePage.toast.close()

      workspacePage.navLink.click()
      workspacePage.toolbox.fit.click()
      // Needed because the fit reduce the visual content of the nodes
      workspacePage.toolbox.zoomIn.click().click()
      workspacePage.bridgeNode('my-bridge').should('be.visible')
      workspacePage.bridgeNode('my-bridge').within(() => {
        cy.getByTestId('connection-status').should('have.text', 'Connected')
      })
      workspacePage.bridgeNode('my-bridge').click()
      workspacePage.toolbar.overview.click()

      workspaceBridgePanel.form.should('be.visible')
      workspaceBridgePanel.modifyBridge.click()
      workspaceBridgePanel.form.should('not.exist')

      bridgePage.config.panel.should('be.visible')
      bridgePage.config.formTab(2).click()

      // This will be item #0
      rjsf.field('localSubscriptions').addItem.click()
      rjsf.field(['localSubscriptions', '0', 'destination']).input.type('my/topic')

      rjsf.field(['localSubscriptions', '0', 'filters']).addItem.click()
      rjsf.field(['localSubscriptions', '0', 'filters', '0']).input.type('first/filter')

      bridgePage.config.submitButton.click()
    })
  })

  it('should be accessible', () => {
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
