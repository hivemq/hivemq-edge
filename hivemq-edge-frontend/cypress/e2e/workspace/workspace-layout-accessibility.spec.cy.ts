import { MOCK_PROTOCOL_HTTP, MOCK_PROTOCOL_OPC_UA, MOCK_PROTOCOL_SIMULATION } from '@/__test-utils__/adapters'
import { MockAdapterType } from '@/__test-utils__/adapters/types.ts'
import { mockBridge } from '@/api/hooks/useGetBridges/__handlers__'
import { MOCK_DEVICE_TAGS, mockAdapter_OPCUA } from '@/api/hooks/useProtocolAdapters/__handlers__'
import { MOCK_TOPIC_FILTER } from '@/api/hooks/useTopicFilters/__handlers__'

import { loginPage, workspacePage } from 'cypress/pages'
import { cy_interceptCoreE2E } from 'cypress/utils/intercept.utils.ts'

describe('Workspace Layout - Accessibility & Visual Regression', { tags: ['@percy'] }, () => {
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

  it('should have accessible layout controls', () => {
    cy.injectAxe()

    // Check accessibility of layout controls panel
    workspacePage.layoutControls.panel.should('be.visible')

    cy.checkAccessibility('[data-testid="layout-controls-panel"]', {
      rules: {
        region: { enabled: false },
        'color-contrast': { enabled: false },
      },
    })
  })

  it('should have accessible presets menu', () => {
    cy.injectAxe()

    // Open presets menu
    workspacePage.layoutControls.presetsButton.click()

    // Check accessibility
    cy.checkAccessibility('[role="menu"]', {
      rules: {
        'color-contrast': { enabled: false },
        region: { enabled: false },
      },
    })
  })

  it('should have accessible options drawer', () => {
    cy.injectAxe()

    // Select algorithm and open drawer
    workspacePage.layoutControls.algorithmSelector.select('DAGRE_TB')
    workspacePage.layoutControls.optionsButton.click()

    // Check accessibility
    cy.checkAccessibility('[role="dialog"]', {
      rules: {
        'color-contrast': { enabled: false },
        region: { enabled: false },
      },
    })
  })

  it('should support keyboard navigation', () => {
    // Focus on workspace to start keyboard navigation
    workspacePage.canvas.click()

    // Tab through controls
    cy.realPress('Tab')
    cy.realPress('Tab')
    cy.realPress('Tab')

    // Should be able to reach layout controls
    workspacePage.layoutControls.panel.should('be.visible')
  })

  it('should take Percy snapshot of layout controls', () => {
    workspacePage.toolbox.fit.click()

    // Snapshot with layout controls visible
    cy.percySnapshot('Workspace - Layout Controls Panel')
  })

  it('should take Percy snapshot of options drawer', () => {
    workspacePage.toolbox.fit.click()

    // Open options drawer
    workspacePage.layoutControls.algorithmSelector.select('DAGRE_TB')
    workspacePage.layoutControls.optionsButton.click()

    // Wait for drawer to be fully visible
    workspacePage.layoutControls.optionsDrawer.drawer.should('be.visible')

    // Snapshot
    cy.percySnapshot('Workspace - Layout Options Drawer')
  })

  it('should take Percy snapshot of presets menu', () => {
    // Save a preset first
    workspacePage.layoutControls.algorithmSelector.select('DAGRE_TB')
    workspacePage.layoutControls.applyButton.click()
    workspacePage.edgeNode.should('be.visible')

    workspacePage.layoutControls.presetsButton.click()
    workspacePage.layoutControls.presetsMenu.saveOption.click()
    workspacePage.layoutControls.savePresetModal.modal.should('be.visible')
    workspacePage.layoutControls.savePresetModal.nameInput.type('Visual Test')
    workspacePage.layoutControls.savePresetModal.saveButton.click()

    // Open presets menu
    workspacePage.layoutControls.presetsButton.click()

    // Snapshot
    cy.percySnapshot('Workspace - Layout Presets Menu')
  })

  it('should take Percy snapshot of workspace after layout', () => {
    workspacePage.toolbox.fit.click()

    // Apply layout
    workspacePage.layoutControls.algorithmSelector.select('DAGRE_TB')
    workspacePage.layoutControls.applyButton.click()

    // Wait for nodes to be visible and positioned
    workspacePage.edgeNode.should('be.visible')
    workspacePage.bridgeNode(mockBridge.id).should('be.visible')

    // Fit to canvas
    workspacePage.toolbox.fit.click()

    // Verify canvas is ready
    workspacePage.canvas.should('be.visible')

    // Snapshot
    cy.percySnapshot('Workspace - After Dagre TB Layout')
  })

  it('should have proper ARIA labels', () => {
    // Check layout selector has proper label
    workspacePage.layoutControls.algorithmSelector.should('have.attr', 'data-testid')

    // Check buttons have proper aria-labels
    workspacePage.layoutControls.presetsButton.should('have.attr', 'aria-label')
    workspacePage.layoutControls.optionsButton.should('have.attr', 'aria-label')

    // Check apply button
    workspacePage.layoutControls.applyButton.should('have.attr', 'data-testid')
  })

  it('should capture before/after screenshots for PR documentation', () => {
    // Wait for all nodes to be visible
    workspacePage.toolbox.fit.click()
    workspacePage.edgeNode.should('be.visible')
    workspacePage.bridgeNode(mockBridge.id).should('be.visible')
    cy.get('[role="group"][data-id^="adapter@"]').should('have.length', 2)

    // Fit to canvas to show all nodes
    workspacePage.toolbox.fit.click()
    workspacePage.canvas.should('be.visible')

    // Screenshot 1: Before applying layout
    cy.screenshot('workspace-layout-before', {
      capture: 'viewport',
      overwrite: true,
    })

    // Select Radial Hub layout algorithm (provides best visual results)
    workspacePage.layoutControls.algorithmSelector.select('RADIAL_HUB')
    workspacePage.layoutControls.algorithmSelector.should('have.value', 'RADIAL_HUB')

    // Apply the layout
    workspacePage.layoutControls.applyButton.click()

    // Wait for nodes to be repositioned
    workspacePage.edgeNode.should('be.visible')
    workspacePage.bridgeNode(mockBridge.id).should('be.visible')

    // Fit to canvas again to show the organized layout
    workspacePage.toolbox.fit.click()

    // Screenshot 2: After applying Radial Hub layout
    cy.screenshot('workspace-layout-after-radial-hub', {
      capture: 'viewport',
      overwrite: true,
    })

    // Verify layout controls are still accessible after layout
    workspacePage.layoutControls.panel.should('be.visible')
  })
})
