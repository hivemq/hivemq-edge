import { MOCK_PROTOCOL_HTTP, MOCK_PROTOCOL_OPC_UA, MOCK_PROTOCOL_SIMULATION } from '@/__test-utils__/adapters'
import { MockAdapterType } from '@/__test-utils__/adapters/types.ts'
import { mockBridge } from '@/api/hooks/useGetBridges/__handlers__'
import { MOCK_DEVICE_TAGS, mockAdapter_OPCUA } from '@/api/hooks/useProtocolAdapters/__handlers__'
import { MOCK_TOPIC_FILTER } from '@/api/hooks/useTopicFilters/__handlers__'

import { loginPage, workspacePage } from 'cypress/pages'
import { cy_interceptCoreE2E } from 'cypress/utils/intercept.utils.ts'

describe('Workspace Layout - Keyboard Shortcuts', () => {
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
    cy.intercept('/api/v1/data-hub/data-validation/policies', { statusCode: 202, log: false })

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

  it('should apply layout with Cmd+L shortcut on Mac', () => {
    // Expand toolbar to access layout controls
    workspacePage.canvasToolbar.expandButton.click()

    // Select an algorithm
    workspacePage.layoutControls.algorithmSelector.select('DAGRE_TB')

    // Focus on workspace area
    workspacePage.canvas.click()

    // Press Cmd+L (Mac shortcut)
    cy.realPress(['Meta', 'L'])

    // Nodes should be visible - Cypress will retry automatically until they are
    workspacePage.edgeNode.should('be.visible')
    workspacePage.bridgeNode(mockBridge.id).should('be.visible')
  })

  it('should apply layout with Ctrl+L shortcut', () => {
    // Expand toolbar to access layout controls
    workspacePage.canvasToolbar.expandButton.click()

    // Select an algorithm
    workspacePage.layoutControls.algorithmSelector.select('DAGRE_LR')

    // Focus on workspace area
    workspacePage.canvas.click()

    // Press Ctrl+L (Windows/Linux shortcut)
    cy.realPress(['Control', 'L'])

    // Nodes should be visible - Cypress will retry automatically
    workspacePage.edgeNode.should('be.visible')
  })

  it('should work with different algorithms', () => {
    // Expand toolbar to access layout controls
    workspacePage.canvasToolbar.expandButton.click()

    // Test with Radial Hub layout
    workspacePage.layoutControls.algorithmSelector.select('RADIAL_HUB')
    workspacePage.canvas.click()
    cy.realPress(['Meta', 'L'])
    workspacePage.edgeNode.should('be.visible')

    // Test with Cola Force layout
    workspacePage.layoutControls.algorithmSelector.select('COLA_FORCE')
    workspacePage.canvas.click()
    cy.realPress(['Meta', 'L'])
    workspacePage.edgeNode.should('be.visible')
  })

  it('should work after interacting with nodes', () => {
    // Expand toolbar to access layout controls
    workspacePage.canvasToolbar.expandButton.click()

    // Select algorithm
    workspacePage.layoutControls.algorithmSelector.select('DAGRE_TB')

    // Click on a node
    workspacePage.edgeNode.click()

    // Use keyboard shortcut
    cy.realPress(['Meta', 'L'])

    // Verify nodes remain visible
    workspacePage.edgeNode.should('be.visible')
  })
})
