import { MOCK_PROTOCOL_HTTP, MOCK_PROTOCOL_OPC_UA, MOCK_PROTOCOL_SIMULATION } from '@/__test-utils__/adapters'
import { MockAdapterType } from '@/__test-utils__/adapters/types.ts'
import { mockBridge } from '@/api/hooks/useGetBridges/__handlers__'
import { MOCK_DEVICE_TAGS, mockAdapter_OPCUA } from '@/api/hooks/useProtocolAdapters/__handlers__'
import { MOCK_TOPIC_FILTER } from '@/api/hooks/useTopicFilters/__handlers__'

import { loginPage, workspacePage } from 'cypress/pages'
import { cy_interceptCoreE2E } from 'cypress/utils/intercept.utils.ts'

describe('Workspace Layout - Presets', () => {
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

  it('should show no saved presets initially', () => {
    // Expand toolbar to access layout controls
    workspacePage.canvasToolbar.expandButton.click()

    // Open presets menu
    workspacePage.layoutControls.presetsButton.click()

    // Should show empty state
    workspacePage.layoutControls.presetsMenu.emptyMessage.should('be.visible')
  })

  it('should open save preset modal', { tags: ['@flaky'] }, () => {
    // Expand toolbar to access layout controls
    workspacePage.canvasToolbar.expandButton.click()

    // Open presets menu
    workspacePage.layoutControls.presetsButton.click()

    // Click "Save Current Layout"
    workspacePage.layoutControls.presetsMenu.saveOption.click()

    // Modal should open
    workspacePage.layoutControls.savePresetModal.modal.should('be.visible')
    workspacePage.layoutControls.savePresetModal.nameInput.should('be.visible')
  })

  it('should require preset name', () => {
    // Expand toolbar to access layout controls
    workspacePage.canvasToolbar.expandButton.click()

    // Open save modal
    workspacePage.layoutControls.presetsButton.click()
    workspacePage.layoutControls.presetsMenu.saveOption.click()

    // Try to save without name
    workspacePage.layoutControls.savePresetModal.saveButton.click()

    // Should show validation message (toast or error)
    // Modal should still be open
    workspacePage.layoutControls.savePresetModal.modal.should('be.visible')
  })

  it('should save a preset with valid name', () => {
    // Expand toolbar to access layout controls
    workspacePage.canvasToolbar.expandButton.click()

    // Apply a layout first
    workspacePage.layoutControls.algorithmSelector.select('DAGRE_TB')
    workspacePage.layoutControls.applyButton.click()
    workspacePage.edgeNode.should('be.visible')

    // Open save modal
    workspacePage.layoutControls.presetsButton.click()
    workspacePage.layoutControls.presetsMenu.saveOption.click()

    // Enter preset name
    workspacePage.layoutControls.savePresetModal.nameInput.type('My Test Layout')

    // Save preset
    workspacePage.layoutControls.savePresetModal.saveButton.click()

    // Modal should close
    workspacePage.layoutControls.savePresetModal.modal.should('not.exist')

    // Open menu again to verify preset was saved
    workspacePage.layoutControls.presetsButton.click()

    // Preset should appear in menu
    cy.get('[role="menu"]').should('contain.text', 'My Test Layout')
  })

  it('should load a saved preset', () => {
    // Expand toolbar to access layout controls
    workspacePage.canvasToolbar.expandButton.click()

    // First, save a preset
    workspacePage.layoutControls.algorithmSelector.select('DAGRE_TB')
    workspacePage.layoutControls.applyButton.click()
    workspacePage.edgeNode.should('be.visible')

    workspacePage.layoutControls.presetsButton.click()
    workspacePage.layoutControls.presetsMenu.saveOption.click()
    workspacePage.layoutControls.savePresetModal.nameInput.type('Load Test')
    workspacePage.layoutControls.savePresetModal.saveButton.click()

    // Apply a different layout to change positions
    workspacePage.layoutControls.algorithmSelector.select('RADIAL_HUB')
    workspacePage.layoutControls.applyButton.click()
    workspacePage.edgeNode.should('be.visible')

    // Load the saved preset
    workspacePage.layoutControls.presetsButton.click()
    workspacePage.layoutControls.presetsMenu.presetItem('Load Test').click()

    // Nodes should be visible (positions restored)
    workspacePage.edgeNode.should('be.visible')
    workspacePage.bridgeNode(mockBridge.id).should('be.visible')
  })

  it('should delete a preset', () => {
    workspacePage.canvasToolbar.expandButton.click()
    // Save a preset
    workspacePage.layoutControls.algorithmSelector.select('DAGRE_TB')
    workspacePage.layoutControls.applyButton.click()
    workspacePage.edgeNode.should('be.visible')

    workspacePage.layoutControls.presetsButton.click()
    workspacePage.layoutControls.presetsMenu.saveOption.click()
    workspacePage.layoutControls.savePresetModal.nameInput.type('Delete Test')
    workspacePage.layoutControls.savePresetModal.saveButton.click()

    // Open presets menu
    workspacePage.layoutControls.presetsButton.click()

    // Click delete button for the preset
    workspacePage.layoutControls.presetsMenu.presetItem('Delete Test').should('be.visible')

    workspacePage.layoutControls.presetsMenu.presetItemDelete('Delete Test').click()

    // Preset should be removed
    workspacePage.layoutControls.presetsButton.click()
    workspacePage.layoutControls.presetsMenu.emptyMessage.should('be.visible')
  })

  it('should persist presets across page reloads', () => {
    workspacePage.canvasToolbar.expandButton.click()
    // Save a preset
    workspacePage.layoutControls.algorithmSelector.select('DAGRE_TB')
    workspacePage.layoutControls.applyButton.click()
    workspacePage.edgeNode.should('be.visible')

    workspacePage.layoutControls.presetsButton.click()
    workspacePage.layoutControls.presetsMenu.saveOption.click()
    workspacePage.layoutControls.savePresetModal.nameInput.type('Persistent Test')
    workspacePage.layoutControls.savePresetModal.saveButton.click()

    // Reload page
    cy.reload()
    loginPage.visit('/app/workspace')
    loginPage.loginButton.click()
    workspacePage.navLink.click()

    cy.wait('@getAdapters')
    cy.wait('@getBridges')
    workspacePage.toolbox.fit.click()

    workspacePage.canvasToolbar.expandButton.click()
    workspacePage.layoutControls.presetsButton.click()
    cy.get('[role="menu"]').should('contain.text', 'Persistent Test')
  })
})
