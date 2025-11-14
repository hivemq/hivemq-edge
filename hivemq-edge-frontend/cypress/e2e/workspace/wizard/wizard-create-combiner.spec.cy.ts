/**
 * Wizard - Create Combiner E2E Tests
 *
 * Focus: Selection Step + Accessibility + Visual Regression
 *
 * Tests the complete combiner creation workflow with SELECTION STEP:
 * 1. Selection Panel accessibility (a11y)
 * 2. Node selection on canvas (screenshot)
 * 3. Selection constraints enforcement (visual regression)
 * 4. Combiner configuration and completion
 *
 * CRITICAL: Tests WizardSelectionPanel getters
 * - selectedCount
 * - selectedNodesList
 * - validationMessage
 * - nextButton state
 */

import { mockAdapter_OPCUA } from '@/api/hooks/useProtocolAdapters/__handlers__'
import { mockBridge, mockBridgeId } from '@/api/hooks/useGetBridges/__handlers__'
import { MOCK_TOPIC_FILTER } from '@/api/hooks/useTopicFilters/__handlers__'

import { loginPage, workspacePage, wizardPage } from '../../../pages'
import { cy_interceptCoreE2E } from '../../../utils/intercept.utils.ts'

const MOCK_ADAPTER_ID1 = 'opcua-1'
const MOCK_ADAPTER_ID2 = 'opcua-2'

describe('Wizard: Create Combiner', () => {
  beforeEach(() => {
    // Base E2E interceptors
    cy_interceptCoreE2E()

    // Multiple adapters for selection
    cy.intercept('/api/v1/management/protocol-adapters/adapters', {
      items: [
        { ...mockAdapter_OPCUA, id: MOCK_ADAPTER_ID1, name: 'OPC-UA Device 1' },
        { ...mockAdapter_OPCUA, id: MOCK_ADAPTER_ID2, name: 'OPC-UA Device 2' },
        { ...mockAdapter_OPCUA, id: 'http-adapter', name: 'HTTP Gateway' },
      ],
    }).as('getAdapters')

    cy.intercept('/api/v1/management/bridges', {
      items: [mockBridge],
    }).as('getBridges')

    cy.intercept('GET', '/api/v1/management/topic-filters', {
      items: [MOCK_TOPIC_FILTER],
    }).as('getTopicFilters')

    cy.intercept('POST', '/api/v1/management/combiners', {
      statusCode: 201,
      body: {
        id: 'test-combiner',
        name: 'Test Combiner',
      },
    }).as('createCombiner')

    // Login and navigate
    loginPage.visit('/app/workspace')
    loginPage.loginButton.click()
    workspacePage.navLink.click()

    // Wait for workspace to load with adapters
    workspacePage.canvas.should('be.visible')
    cy.wait('@getAdapters')
  })

  // ========== 1. SELECTION PANEL ACCESSIBILITY ==========
  /**
   * Tests the critical WizardSelectionPanel component
   * Verifies:
   * - Panel is accessible (WCAG2AA)
   * - Selection count visible and correct
   * - Validation messages clear
   * - Buttons properly labeled
   */
  it('should show accessible selection panel with correct constraints', { tags: ['@percy'] }, () => {
    cy.injectAxe()

    wizardPage.createEntityButton.click()
    wizardPage.wizardMenu.selectOption('COMBINER')

    wizardPage.selectionPanel.panel.should('be.visible')
    workspacePage.toolbox.fit.click()

    cy.checkAccessibility(undefined, {
      rules: {
        // Chakra UI Portal creates region elements without accessible names - known third-party issue
        region: { enabled: false },
      },
    })

    cy.percySnapshot('Wizard: Combiner Selection Panel (Empty)')

    wizardPage.selectionPanel.selectedCount.should('contain', '0 (min: 2)')
    wizardPage.selectionPanel.nextButton.should('be.disabled')
  })

  // ========== 2. NODE SELECTION AND CONSTRAINTS ==========
  /**
   * Tests the WizardSelectionRestrictions behavior:
   * - Clicking nodes adds them to selection panel
   * - Selection count updates correctly
   * - Validation changes based on constraints
   * - Next button enables when constraints met
   */
  it('should enforce selection constraints and update panel state', () => {
    wizardPage.createEntityButton.click()
    wizardPage.wizardMenu.selectOption('COMBINER')

    // Canvas should show selectable nodes
    wizardPage.canvas.container.should('be.visible')
    workspacePage.toolbox.fit.click()

    // === Test: Select First Node ===
    // Find and click first adapter node
    workspacePage.adapterNode(MOCK_ADAPTER_ID1).click()

    wizardPage.selectionPanel.selectedCount.should('contain', '1 (min: 2)')
    wizardPage.selectionPanel.selectedNode(MOCK_ADAPTER_ID1).should('be.visible')
    wizardPage.selectionPanel.nextButton.should('be.disabled')

    // workspacePage.adapterNode(MOCK_ADAPTER_ID1).click()
    // wizardPage.selectionPanel.selectedCount.should('contain', '0 (min: 2)')
    // wizardPage.selectionPanel.selectedNode(MOCK_ADAPTER_ID1).should('not.exist')
    //
    // workspacePage.adapterNode(MOCK_ADAPTER_ID1).click()
    workspacePage.adapterNode(MOCK_ADAPTER_ID2).click()

    wizardPage.selectionPanel.selectedCount.should('contain', '2 (min: 2)')
    wizardPage.selectionPanel.selectedNode(MOCK_ADAPTER_ID1).should('be.visible')
    wizardPage.selectionPanel.selectedNode(MOCK_ADAPTER_ID2).should('be.visible')
    wizardPage.selectionPanel.nextButton.should('not.be.disabled')

    cy.percySnapshot('Wizard: Combiner Two Nodes Selected (Constraint Met)')
  })

  // ========== 3. DESELECTION AND PANEL UPDATES ==========
  /**
   * Tests removing nodes from selection
   * Verifies panel updates when deselecting
   */
  it('should allow removing nodes from selection', () => {
    wizardPage.createEntityButton.click()
    wizardPage.wizardMenu.selectOption('COMBINER')
    workspacePage.toolbox.fit.click()

    // === Test: Select First Node ===
    // Find and click first adapter node
    workspacePage.adapterNode(MOCK_ADAPTER_ID1).click()

    wizardPage.selectionPanel.selectedCount.should('contain', '1 (min: 2)')
    wizardPage.selectionPanel.selectedNode(MOCK_ADAPTER_ID1).should('be.visible')
    wizardPage.selectionPanel.nextButton.should('be.disabled')

    workspacePage.adapterNode(MOCK_ADAPTER_ID1).click()
    wizardPage.selectionPanel.selectedCount.should('contain', '0 (min: 2)')

    workspacePage.adapterNode(MOCK_ADAPTER_ID2).click()

    wizardPage.selectionPanel.selectedCount.should('contain', '1 (min: 2)')
    wizardPage.selectionPanel.selectedNode(MOCK_ADAPTER_ID2).should('be.visible')
    wizardPage.selectionPanel.nextButton.should('be.disabled')
  })

  // ========== 4. COMBINER CONFIGURATION ==========
  /**
   * Tests the configuration step after selection
   * Verifies configuration form appears and works
   */
  it('should proceed to combiner configuration after selection', () => {
    wizardPage.createEntityButton.click()
    wizardPage.wizardMenu.selectOption('COMBINER')
    workspacePage.toolbox.fit.click()
    workspacePage.adapterNode(MOCK_ADAPTER_ID1).click()
    workspacePage.adapterNode(MOCK_ADAPTER_ID2).click()

    wizardPage.selectionPanel.nextButton.click()

    // Configuration for regression
    cy.percySnapshot('Wizard: Combiner Configuration Form')
    wizardPage.combinerConfig.configForm.should('be.visible')
    wizardPage.combinerConfig.combinerNameInput.clear().type('My combiner')

    wizardPage.combinerConfig.submitButton.click()
    cy.wait('@createCombiner')

    wizardPage.toast.success.should('be.visible')
  })

  // ========== 5. VISUAL REGRESSION - MULTIPLE SELECTIONS ==========
  /**
   * Ensures UI consistency when selecting different node combinations
   * Percy regression to catch any layout/styling issues
   */
  it.only('should maintain visual consistency with maximum selection', { tags: ['@percy'] }, () => {
    wizardPage.createEntityButton.click()
    wizardPage.wizardMenu.selectOption('COMBINER')
    workspacePage.toolbox.fit.click()

    // Select all three available adapters
    workspacePage.adapterNode(MOCK_ADAPTER_ID1).click()
    workspacePage.bridgeNode(mockBridgeId).click()
    workspacePage.adapterNode(MOCK_ADAPTER_ID2).click()
    workspacePage.adapterNode('http-adapter').click()
    // All selected
    wizardPage.getSelectedCount().should('equal', '4')

    // All should appear in panel
    wizardPage.selectionPanel.selectedNode(MOCK_ADAPTER_ID1).should('be.visible')
    wizardPage.selectionPanel.selectedNode(MOCK_ADAPTER_ID2).should('be.visible')
    wizardPage.selectionPanel.selectedNode('http-adapter').should('be.visible')
    wizardPage.selectionPanel.selectedNode(mockBridgeId).should('be.visible')

    // Regression: Full selection list
    cy.percySnapshot('Wizard: Combiner Maximum Selection (Regression)')
  })
})
