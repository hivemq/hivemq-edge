import { MOCK_PROTOCOL_HTTP, MOCK_PROTOCOL_OPC_UA, MOCK_PROTOCOL_SIMULATION } from '@/__test-utils__/adapters'
import { MockAdapterType } from '@/__test-utils__/adapters/types.ts'
import { mockBridge } from '@/api/hooks/useGetBridges/__handlers__'
import { MOCK_DEVICE_TAGS, mockAdapter_OPCUA } from '@/api/hooks/useProtocolAdapters/__handlers__'
import { MOCK_TOPIC_FILTER } from '@/api/hooks/useTopicFilters/__handlers__'

import { loginPage, workspacePage } from 'cypress/pages'
import { cy_interceptCoreE2E } from 'cypress/utils/intercept.utils.ts'

describe('Workspace Layout - Basic', () => {
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

  it('should display layout controls in workspace', () => {
    // Layout controls panel should be visible
    workspacePage.layoutControls.panel.should('be.visible')

    // All control elements should be present
    workspacePage.layoutControls.algorithmSelector.should('be.visible')
    workspacePage.layoutControls.applyButton.should('be.visible')
    workspacePage.layoutControls.presetsButton.should('be.visible')
    workspacePage.layoutControls.optionsButton.should('be.visible')
  })

  it('should allow selecting different layout algorithms', () => {
    // Select Vertical Tree Layout (Dagre TB)
    workspacePage.layoutControls.algorithmSelector.select('DAGRE_TB')
    workspacePage.layoutControls.algorithmSelector.should('have.value', 'DAGRE_TB')

    // Select Horizontal Tree Layout (Dagre LR)
    workspacePage.layoutControls.algorithmSelector.select('DAGRE_LR')
    workspacePage.layoutControls.algorithmSelector.should('have.value', 'DAGRE_LR')

    // Select Force-Directed Layout
    workspacePage.layoutControls.algorithmSelector.select('COLA_FORCE')
    workspacePage.layoutControls.algorithmSelector.should('have.value', 'COLA_FORCE')
  })

  it('should apply layout when button clicked', () => {
    // Get initial positions of nodes
    let initialEdgePosition: { x: number; y: number }
    let initialBridgePosition: { x: number; y: number }

    workspacePage.edgeNode.then(($edge) => {
      const transform = $edge.attr('transform')
      const match = transform?.match(/translate\(([^,]+),([^)]+)\)/)
      if (match) {
        initialEdgePosition = { x: parseFloat(match[1]), y: parseFloat(match[2]) }
      }
    })

    workspacePage.bridgeNode(mockBridge.id).then(($bridge) => {
      const transform = $bridge.attr('transform')
      const match = transform?.match(/translate\(([^,]+),([^)]+)\)/)
      if (match) {
        initialBridgePosition = { x: parseFloat(match[1]), y: parseFloat(match[2]) }
      }
    })

    // Select an algorithm and apply layout
    workspacePage.layoutControls.algorithmSelector.select('DAGRE_TB')
    workspacePage.layoutControls.applyButton.click()

    // Verify nodes remain visible after layout (Cypress will retry automatically)
    workspacePage.edgeNode.should('be.visible')

    // Verify both nodes have moved (positions should have changed)
    workspacePage.edgeNode.then(($edge) => {
      const transform = $edge.attr('transform')
      const match = transform?.match(/translate\(([^,]+),([^)]+)\)/)
      if (match && initialEdgePosition) {
        const newX = parseFloat(match[1])
        const newY = parseFloat(match[2])
        // At least one coordinate should have changed
        expect(newX !== initialEdgePosition.x || newY !== initialEdgePosition.y).to.be.true
      }
    })

    workspacePage.bridgeNode(mockBridge.id).then(($bridge) => {
      const transform = $bridge.attr('transform')
      const match = transform?.match(/translate\(([^,]+),([^)]+)\)/)
      if (match && initialBridgePosition) {
        const newX = parseFloat(match[1])
        const newY = parseFloat(match[2])
        // At least one coordinate should have changed
        expect(newX !== initialBridgePosition.x || newY !== initialBridgePosition.y).to.be.true
      }
    })
  })

  it('should apply multiple layouts in sequence', () => {
    // Apply first layout
    workspacePage.layoutControls.algorithmSelector.select('DAGRE_TB')
    workspacePage.layoutControls.applyButton.click()
    workspacePage.edgeNode.should('be.visible')

    // Apply second layout
    workspacePage.layoutControls.algorithmSelector.select('DAGRE_LR')
    workspacePage.layoutControls.applyButton.click()
    workspacePage.edgeNode.should('be.visible')

    // Apply third layout
    workspacePage.layoutControls.algorithmSelector.select('RADIAL_HUB')
    workspacePage.layoutControls.applyButton.click()

    // Workspace should still be functional
    workspacePage.edgeNode.should('be.visible')
    workspacePage.bridgeNode(mockBridge.id).should('be.visible')
  })

  it('should persist selected algorithm across interactions', () => {
    // Select an algorithm
    workspacePage.layoutControls.algorithmSelector.select('COLA_FORCE')

    // Click on a node
    workspacePage.edgeNode.click()

    // Algorithm selection should still be the same
    workspacePage.layoutControls.algorithmSelector.should('have.value', 'COLA_FORCE')
  })
})
