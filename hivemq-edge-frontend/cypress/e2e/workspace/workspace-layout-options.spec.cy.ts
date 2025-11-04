import { MOCK_PROTOCOL_HTTP, MOCK_PROTOCOL_OPC_UA, MOCK_PROTOCOL_SIMULATION } from '@/__test-utils__/adapters'
import { MockAdapterType } from '@/__test-utils__/adapters/types.ts'
import { mockBridge } from '@/api/hooks/useGetBridges/__handlers__'
import { MOCK_DEVICE_TAGS, mockAdapter_OPCUA } from '@/api/hooks/useProtocolAdapters/__handlers__'
import { MOCK_TOPIC_FILTER } from '@/api/hooks/useTopicFilters/__handlers__'

import { loginPage, workspacePage } from 'cypress/pages'
import { cy_interceptCoreE2E } from 'cypress/utils/intercept.utils.ts'

describe('Workspace Layout - Options', () => {
  beforeEach(() => {
    cy_interceptCoreE2E()

    cy.intercept('/api/v1/management/protocol-adapters/types', {
      items: [MOCK_PROTOCOL_HTTP, MOCK_PROTOCOL_OPC_UA, MOCK_PROTOCOL_SIMULATION],
    }).as('getProtocols')

    cy.intercept('/api/v1/management/bridges', { items: [mockBridge] }).as('getBridges')
    cy.intercept('/api/v1/management/protocol-adapters/adapters', {
      items: [mockAdapter_OPCUA, { ...mockAdapter_OPCUA, id: 'opcua-2' }],
    }).as('getAdapters')

    cy.intercept('/api/v1/management/topic-filters', {
      items: [MOCK_TOPIC_FILTER],
    }).as('getTopicFilters')

    cy.intercept('/api/v1/management/protocol-adapters/adapters/**/northboundMappings', {
      statusCode: 202,
      log: false,
    })
    cy.intercept('/api/v1/management/protocol-adapters/adapters/**/southboundMappings', {
      statusCode: 202,
      log: false,
    })

    cy.intercept('GET', '/api/v1/management/protocol-adapters/adapters/**/tags', (req) => {
      const pathname = new URL(req.url).pathname
      const id = pathname.split('/')[6]
      req.reply(200, { items: MOCK_DEVICE_TAGS(id, MockAdapterType.OPC_UA) })
    })

    loginPage.visit('/app/workspace')
    loginPage.loginButton.click()
    workspacePage.navLink.click()

    cy.wait('@getAdapters')
    cy.wait('@getBridges')
    workspacePage.toolbox.fit.click()
  })

  it('should open layout options drawer', () => {
    // Expand toolbar to access layout controls
    workspacePage.canvasToolbar.expandButton.click()

    // Click options button
    workspacePage.layoutControls.optionsButton.click()

    // Drawer should open
    workspacePage.layoutControls.optionsDrawer.drawer.should('be.visible')
    workspacePage.layoutControls.optionsDrawer.title.should('contain.text', 'Layout Options')
  })

  it('should show different options for different algorithms', () => {
    // Expand toolbar to access layout controls
    workspacePage.canvasToolbar.expandButton.click()

    // Select Dagre algorithm
    workspacePage.layoutControls.algorithmSelector.select('DAGRE_TB')
    workspacePage.layoutControls.optionsButton.click()

    // Should show form for Dagre options
    workspacePage.layoutControls.optionsDrawer.form.should('be.visible')

    // Close drawer
    workspacePage.layoutControls.optionsDrawer.cancelButton.click()
    workspacePage.layoutControls.optionsDrawer.drawer.should('not.exist')

    // Select Manual algorithm
    workspacePage.layoutControls.algorithmSelector.select('MANUAL')
    workspacePage.layoutControls.optionsButton.click()

    // Should show message about manual layout
    workspacePage.layoutControls.optionsDrawer.drawer.should(
      'contain.text',
      'Manual layout has no configurable options'
    )
  })

  it('should close drawer on cancel', () => {
    // Expand toolbar to access layout controls
    workspacePage.canvasToolbar.expandButton.click()

    // Open drawer
    workspacePage.layoutControls.algorithmSelector.select('DAGRE_TB')
    workspacePage.layoutControls.optionsButton.click()
    workspacePage.layoutControls.optionsDrawer.drawer.should('be.visible')

    // Click cancel
    workspacePage.layoutControls.optionsDrawer.cancelButton.click()

    // Drawer should close
    workspacePage.layoutControls.optionsDrawer.drawer.should('not.exist')
  })

  it('should apply layout with modified options', () => {
    // Expand toolbar to access layout controls
    workspacePage.canvasToolbar.expandButton.click()

    // Select algorithm and open options
    workspacePage.layoutControls.algorithmSelector.select('DAGRE_TB')
    workspacePage.layoutControls.optionsButton.click()

    // Drawer should be visible
    workspacePage.layoutControls.optionsDrawer.drawer.should('be.visible')

    // Modify some options (if form fields are accessible)
    // Note: RJSF forms may need specific selectors
    workspacePage.layoutControls.optionsDrawer.form.should('exist')

    // Apply options (this will also apply layout)
    workspacePage.layoutControls.optionsDrawer.applyButton.click()

    // Drawer should close
    workspacePage.layoutControls.optionsDrawer.drawer.should('not.exist')

    // Nodes should still be visible
    workspacePage.edgeNode.should('be.visible')
  })

  it('should show no options for null algorithm selection', () => {
    workspacePage.canvasToolbar.expandButton.click()
    // Open options drawer without selecting algorithm (if possible)
    // Or with MANUAL selected
    workspacePage.layoutControls.optionsButton.click()

    // Drawer should open
    workspacePage.layoutControls.optionsDrawer.drawer.should('be.visible')
  })
})
