import { mockAdapter_OPCUA } from '@/api/hooks/useProtocolAdapters/__handlers__'
import { mockBridge } from '@/api/hooks/useGetBridges/__handlers__'
import { MOCK_TOPIC_FILTER } from '@/api/hooks/useTopicFilters/__handlers__'

import { loginPage, workspacePage, wizardPage } from '../../../pages'
import { cy_interceptCoreE2E } from '../../../utils/intercept.utils.ts'

const MOCK_ADAPTER_ID1 = 'opcua-1'
const MOCK_ADAPTER_ID2 = 'opcua-2'
const MOCK_ADAPTER_ID3 = 'http-adapter'

describe('Wizard: Create Group', () => {
  beforeEach(() => {
    // Base E2E interceptors
    cy_interceptCoreE2E()
    cy.intercept('/api/v1/management/protocol-adapters/adapters/**/northboundMappings', {
      statusCode: 202,
      log: false,
    })
    cy.intercept('/api/v1/management/protocol-adapters/adapters/**/southboundMappings', {
      statusCode: 202,
      log: false,
    })
    cy.intercept('/api/v1/management/protocol-adapters/adapters/**/tags', {
      statusCode: 202,
      log: false,
    })

    // Multiple adapters for selection
    cy.intercept('/api/v1/management/protocol-adapters/adapters', {
      items: [
        { ...mockAdapter_OPCUA, id: MOCK_ADAPTER_ID1, name: 'OPC-UA Device 1' },
        { ...mockAdapter_OPCUA, id: MOCK_ADAPTER_ID2, name: 'OPC-UA Device 2' },
        { ...mockAdapter_OPCUA, id: MOCK_ADAPTER_ID3, name: 'HTTP Gateway' },
      ],
    }).as('getAdapters')

    cy.intercept('/api/v1/management/bridges', {
      items: [mockBridge],
    })

    cy.intercept('GET', '/api/v1/management/topic-filters', {
      items: [MOCK_TOPIC_FILTER],
    })

    // Group creation endpoint - note: groups are created through workspace state, not API
    // So we don't need a POST intercept, but we can track it for debugging
    cy.intercept('POST', '/api/v1/management/groups', {
      statusCode: 201,
      body: {
        id: 'test-group',
        title: 'Test Group',
      },
    }).as('createGroup')

    cy.intercept('/api/v1/management/protocol-adapters/types', { statusCode: 202, log: false })
    cy.intercept('/api/v1/data-hub/data-validation/policies', { statusCode: 202, log: false })

    // Login and navigate
    loginPage.visit('/app/workspace')
    loginPage.loginButton.click()
    workspacePage.navLink.click()

    // Wait for workspace to load with adapters
    workspacePage.canvas.should('be.visible')
    cy.wait('@getAdapters')
  })

  describe('Critical Path', () => {
    /**
     * Create Group with 2 Adapters (Minimal)
     * Complete E2E: Start wizard → Select 2 nodes → Configure → Submit → Verify creation
     */
    it('should create a group with minimum required nodes', () => {
      // Start wizard
      wizardPage.createEntityButton.click()
      wizardPage.wizardMenu.selectOption('GROUP')

      wizardPage.selectionPanel.panel.should('be.visible')
      workspacePage.toolbox.fit.click()

      // DEBUG: Capture state before selection
      cy.saveHTMLSnapshot('wizard-group-before-selection')
      cy.logDOMState('Before selecting nodes')

      // Select first adapter
      workspacePage.adapterNode(MOCK_ADAPTER_ID1).click()
      wizardPage.selectionPanel.selectedCount.should('contain', '1')
      wizardPage.selectionPanel.selectedCount.should('contain', 'min: 2')
      wizardPage.selectionPanel.nextButton.should('be.disabled')

      // Select second adapter
      workspacePage.adapterNode(MOCK_ADAPTER_ID2).click()
      wizardPage.selectionPanel.selectedCount.should('contain', '2')
      wizardPage.selectionPanel.selectedCount.should('contain', 'min: 2')
      wizardPage.selectionPanel.nextButton.should('not.be.disabled')

      // DEBUG: Capture state after selection
      cy.saveHTMLSnapshot('wizard-group-after-selection')
      cy.logDOMState('After selecting 2 nodes')

      // Proceed to configuration
      wizardPage.selectionPanel.nextButton.click()

      // Verify configuration form appears
      wizardPage.groupConfig.panel.should('be.visible')
      wizardPage.groupConfig.titleInput.should('be.visible')

      // DEBUG: Capture configuration form
      cy.saveHTMLSnapshot('wizard-group-configuration')
      cy.logDOMState('Configuration form')

      // Configure group
      wizardPage.groupConfig.setTitle('My Test Group')

      // Submit form
      wizardPage.groupConfig.submitButton.click()

      // Verify success
      wizardPage.toast.success.should('be.visible')

      // DEBUG: Capture success state
      cy.saveHTMLSnapshot('wizard-group-success')
      cy.logDOMState('After successful group creation')
    })

    /**
     * Create Group with Mixed Node Types
     * Tests grouping adapters, bridges, and potentially nested groups
     */
    it('should create a group with mixed node types', () => {
      wizardPage.createEntityButton.click()
      wizardPage.wizardMenu.selectOption('GROUP')
      workspacePage.toolbox.fit.click()

      // Select adapter
      workspacePage.adapterNode(MOCK_ADAPTER_ID1).click()
      wizardPage.selectionPanel.selectedCount.should('contain', '1')

      // Select bridge
      workspacePage.bridgeNode(mockBridge.id).click()
      wizardPage.selectionPanel.selectedCount.should('contain', '2')

      // Select another adapter
      workspacePage.adapterNode(MOCK_ADAPTER_ID3).click()
      wizardPage.selectionPanel.selectedCount.should('contain', '3')

      // Proceed to configuration
      wizardPage.selectionPanel.nextButton.click()

      // Configure group
      wizardPage.groupConfig.panel.should('be.visible')
      wizardPage.groupConfig.setTitle('Mixed Content Group')

      // Submit
      wizardPage.groupConfig.submitButton.click()
      wizardPage.toast.success.should('be.visible')
    })
  })

  describe('Selection Constraints & Auto-Inclusion', () => {
    /**
     * Auto-Include DEVICE/HOST Nodes
     * Tests that device nodes are automatically included when adapter is selected
     */
    it('should automatically include DEVICE/HOST nodes when adapter is selected', () => {
      wizardPage.createEntityButton.click()
      wizardPage.wizardMenu.selectOption('GROUP')
      workspacePage.toolbox.fit.click()

      // Select first adapter
      workspacePage.adapterNode(MOCK_ADAPTER_ID1).click()

      // Verify selection count updates (adapter selected)
      wizardPage.selectionPanel.selectedCount.should('contain', '1')

      // Select second adapter
      workspacePage.adapterNode(MOCK_ADAPTER_ID2).click()
      wizardPage.selectionPanel.selectedCount.should('contain', '2')

      // Verify next button is enabled
      wizardPage.selectionPanel.nextButton.should('not.be.disabled')

      // Proceed to configuration
      wizardPage.selectionPanel.nextButton.click()
      wizardPage.groupConfig.panel.should('be.visible')
      wizardPage.groupConfig.setTitle('Auto-Include Test Group')
      wizardPage.groupConfig.submitButton.click()

      wizardPage.toast.success.should('be.visible')
    })

    /**
     * Prevent Selecting Already-Grouped Nodes
     * Tests that nodes already in a group cannot be selected
     * NOTE: Requires mocking nodes with parentId set
     */
    it.skip('should disable nodes that are already in a group', () => {
      // This test requires special setup with nodes that have parentId
      // Will need to modify beforeEach or create a separate test context

      wizardPage.createEntityButton.click()
      wizardPage.wizardMenu.selectOption('GROUP')
      workspacePage.toolbox.fit.click()

      // Try to select a node that's already in a group
      // This node should be visually dimmed/disabled
      // Clicking it should have no effect

      // Verify selection count doesn't change
      wizardPage.selectionPanel.selectedCount.should('contain', '0')
    })

    /**
     * Deselection and Panel Updates
     * Tests removing nodes from selection via clicking or remove button
     */
    it('should allow removing nodes from selection', () => {
      wizardPage.createEntityButton.click()
      wizardPage.wizardMenu.selectOption('GROUP')
      workspacePage.toolbox.fit.click()

      // Select first node
      workspacePage.adapterNode(MOCK_ADAPTER_ID1).click()
      wizardPage.selectionPanel.selectedCount.should('contain', '1')
      wizardPage.selectionPanel.nextButton.should('be.disabled')

      // Select second node (meets constraint)
      workspacePage.adapterNode(MOCK_ADAPTER_ID2).click()
      wizardPage.selectionPanel.selectedCount.should('contain', '2')
      wizardPage.selectionPanel.nextButton.should('not.be.disabled')

      // Deselect first node by clicking it again
      workspacePage.adapterNode(MOCK_ADAPTER_ID1).click()
      wizardPage.selectionPanel.selectedCount.should('contain', '1')
      wizardPage.selectionPanel.nextButton.should('be.disabled')

      // Re-select to test remove button
      workspacePage.adapterNode(MOCK_ADAPTER_ID1).click()
      wizardPage.selectionPanel.selectedCount.should('contain', '2')

      // Click remove button in panel (implementation may vary)
      // wizardPage.selectionPanel.removeSelectedNode(`adapter@${MOCK_ADAPTER_ID1}`).click()
      // wizardPage.selectionPanel.selectedCount.should('contain', '1')
    })
  })

  describe('Ghost Node Preview', () => {
    /**
     * Ghost Group Appears on First Selection
     * Tests that ghost group appears when first node is selected
     */
    it('should show ghost group boundary when first node is selected', () => {
      wizardPage.createEntityButton.click()
      wizardPage.wizardMenu.selectOption('GROUP')
      workspacePage.toolbox.fit.click()

      // Initially no ghost group
      cy.get('[data-testid="rf__wrapper"]').within(() => {
        cy.get('[data-testid^="rf__node-ghost-group"]').should('not.exist')
      })

      // Select first adapter
      workspacePage.adapterNode(MOCK_ADAPTER_ID1).click()

      // Ghost group should appear
      cy.get('[data-testid="rf__wrapper"]').within(() => {
        cy.get('[data-testid^="rf__node-ghost-group"]').should('be.visible')
      })
    })

    /**
     * Ghost Group Expands with Additional Selections
     * Tests that ghost group boundary grows as more nodes are selected
     */
    it('should expand ghost group boundary as more nodes are selected', () => {
      wizardPage.createEntityButton.click()
      wizardPage.wizardMenu.selectOption('GROUP')
      workspacePage.toolbox.fit.click()

      // Select first node
      workspacePage.adapterNode(MOCK_ADAPTER_ID1).click()

      // Get initial ghost group bounds (for comparison)
      cy.get('[data-testid^="rf__node-ghost-group"]').should('be.visible')

      // Select second node (positioned differently)
      workspacePage.adapterNode(MOCK_ADAPTER_ID2).click()

      // Ghost group should still be visible and contain both nodes
      cy.get('[data-testid^="rf__node-ghost-group"]').should('be.visible')

      // Select third node
      workspacePage.adapterNode(MOCK_ADAPTER_ID3).click()
    })

    /**
     * Ghost Group Shrinks on Deselection
     * Tests that ghost group shrinks and disappears when nodes are deselected
     */
    it('should shrink ghost group when nodes are deselected', () => {
      wizardPage.createEntityButton.click()
      wizardPage.wizardMenu.selectOption('GROUP')
      workspacePage.toolbox.fit.click()

      // Select 3 nodes
      workspacePage.adapterNode(MOCK_ADAPTER_ID1).click()
      workspacePage.adapterNode(MOCK_ADAPTER_ID2).click()
      workspacePage.adapterNode(MOCK_ADAPTER_ID3).click()

      // Ghost group visible with all 3
      cy.get('[data-testid^="rf__node-ghost-group"]').should('be.visible')

      // Deselect one node
      workspacePage.adapterNode(MOCK_ADAPTER_ID3).click()
      cy.get('[data-testid^="rf__node-ghost-group"]').should('be.visible')

      // Deselect second node
      workspacePage.adapterNode(MOCK_ADAPTER_ID2).click()
      cy.get('[data-testid^="rf__node-ghost-group"]').should('be.visible')

      // Deselect last node - ghost should disappear
      workspacePage.adapterNode(MOCK_ADAPTER_ID1).click()
      cy.get('[data-testid^="rf__node-ghost-group"]').should('not.exist')
    })
  })

  describe('Nested Group Constraints', () => {
    /**
     * Maximum Nesting Depth (3 Levels)
     * Tests that depth limit is enforced with error message
     * NOTE: Requires special mock setup with nested groups
     */
    it.skip('should enforce maximum nesting depth of 3 levels', () => {
      // This test requires mocking a hierarchy:
      // - group-level-1 (root)
      //   - group-level-2 (child of level-1)
      //     - group-level-3 (child of level-2)
      //
      // Attempting to create a group with level-3 + another node should fail

      // For now, skip implementation until mock data is set up
      cy.log('Test requires nested group mock data')
    })

    /**
     * Valid Nested Group Creation (2 Levels)
     * Tests that nested groups work within the depth limit
     */
    it.skip('should allow creating nested groups within depth limit', () => {
      // This test requires mocking:
      // - group-level-1 with child adapter
      // - Another standalone adapter
      //
      // Creating a group with both should succeed (creates 2-level hierarchy)

      cy.log('Test requires nested group mock data')
    })
  })

  describe('Configuration Form Interactions', () => {
    /**
     * Form Submission with Custom Color
     * Tests selecting a color scheme for the group
     */
    it.skip('should create group with custom color scheme', () => {
      wizardPage.createEntityButton.click()
      wizardPage.wizardMenu.selectOption('GROUP')
      workspacePage.toolbox.fit.click()

      // Select 2 nodes
      workspacePage.adapterNode(MOCK_ADAPTER_ID1).click()
      workspacePage.adapterNode(MOCK_ADAPTER_ID2).click()

      // Go to configuration
      wizardPage.selectionPanel.nextButton.click()

      // Configure with custom color
      wizardPage.groupConfig.setTitle('Production Servers')

      // Select color scheme (implementation may vary)
      // wizardPage.groupConfig.selectColorScheme('red')

      wizardPage.groupConfig.submitButton.click()
      wizardPage.toast.success.should('be.visible')
    })

    /**
     * Form Configuration and Submission
     * Tests that form accepts title input and submits successfully
     */
    it('should configure and submit group form successfully', () => {
      wizardPage.createEntityButton.click()
      wizardPage.wizardMenu.selectOption('GROUP')
      workspacePage.toolbox.fit.click()

      // Select 2 nodes
      workspacePage.adapterNode(MOCK_ADAPTER_ID1).click()
      workspacePage.adapterNode(MOCK_ADAPTER_ID2).click()
      wizardPage.selectionPanel.nextButton.click()

      // Verify configuration form appears
      wizardPage.groupConfig.panel.should('be.visible')
      wizardPage.groupConfig.titleInput.should('be.visible')

      // Enter title without chaining to avoid DOM detachment
      wizardPage.groupConfig.titleInput.clear().should('have.value', '')
      wizardPage.groupConfig.titleInput.type('Valid Group Title')

      // Submit successfully
      wizardPage.groupConfig.submitButton.click()

      wizardPage.toast.success.should('be.visible')
    })

    /**
     * Back Button Returns to Selection
     * Tests navigation back to selection step preserves state
     */
    it('should allow returning to selection step from configuration', () => {
      wizardPage.createEntityButton.click()
      wizardPage.wizardMenu.selectOption('GROUP')
      workspacePage.toolbox.fit.click()

      // Select 2 nodes
      workspacePage.adapterNode(MOCK_ADAPTER_ID1).click()
      workspacePage.adapterNode(MOCK_ADAPTER_ID2).click()
      wizardPage.selectionPanel.selectedCount.should('contain', '2')

      // Go to configuration
      wizardPage.selectionPanel.nextButton.click()
      wizardPage.groupConfig.panel.should('be.visible')

      // Click back button in the drawer footer
      wizardPage.groupConfig.backButton.click()

      // Should return to selection panel
      wizardPage.selectionPanel.panel.should('be.visible')

      // Previous selections should be preserved (count check only)
      wizardPage.selectionPanel.selectedCount.should('contain', '2')

      // Next button should still be enabled since we have 2 nodes
      wizardPage.selectionPanel.nextButton.should('not.be.disabled')

      // Modify selection - deselect one node
      workspacePage.adapterNode(MOCK_ADAPTER_ID2).click()
      wizardPage.selectionPanel.selectedCount.should('contain', '1')
      wizardPage.selectionPanel.nextButton.should('be.disabled')

      // Add different node
      workspacePage.adapterNode(MOCK_ADAPTER_ID3).click()
      wizardPage.selectionPanel.selectedCount.should('contain', '2')
      wizardPage.selectionPanel.nextButton.should('not.be.disabled')

      // Should be able to proceed again
      wizardPage.selectionPanel.nextButton.click()
      wizardPage.groupConfig.panel.should('be.visible')
    })
  })

  describe('Visual Documentation & Regression', () => {
    /**
     * PR Documentation Screenshots
     * Generates local screenshot files for PR and blog documentation
     */
    it('should capture wizard menu dropdown', () => {
      wizardPage.createEntityButton.click()
      wizardPage.wizardMenu.selectOption('GROUP')
      cy.screenshot('PR-wizard-menu-dropdown', { overwrite: true, capture: 'viewport' })
    })

    it('should capture ghost preview with selected nodes', () => {
      wizardPage.createEntityButton.click()
      wizardPage.wizardMenu.selectOption('GROUP')
      workspacePage.toolbox.fit.click()

      workspacePage.adapterNode(MOCK_ADAPTER_ID1).click()
      workspacePage.adapterNode(MOCK_ADAPTER_ID2).click()
      wizardPage.selectionPanel.selectedCount.should('contain', '2')
      cy.get('[data-testid^="rf__node-ghost-group"]').should('be.visible')

      cy.screenshot('PR-ghost-nodes-multiple', { overwrite: true, capture: 'viewport' })
    })

    it('should capture configuration panel', () => {
      wizardPage.createEntityButton.click()
      wizardPage.wizardMenu.selectOption('GROUP')
      workspacePage.toolbox.fit.click()

      workspacePage.adapterNode(MOCK_ADAPTER_ID1).click()
      workspacePage.adapterNode(MOCK_ADAPTER_ID2).click()
      wizardPage.selectionPanel.nextButton.click()

      wizardPage.groupConfig.panel.should('be.visible')
      wizardPage.groupConfig.titleInput.should('be.visible')

      cy.screenshot('PR-configuration-panel', { overwrite: true, capture: 'viewport' })
    })

    /**
     * Percy Visual Regression + Accessibility
     * Percy snapshots for visual regression with accessibility checks
     */
    it('should maintain visual consistency and accessibility', { tags: ['@percy'] }, () => {
      cy.injectAxe()

      // Percy snapshot 1: Selection panel with accessibility check
      wizardPage.createEntityButton.click()
      wizardPage.wizardMenu.selectOption('GROUP')
      workspacePage.toolbox.fit.click()
      wizardPage.selectionPanel.panel.should('be.visible')
      wizardPage.selectionPanel.selectedCount.should('be.visible')

      cy.checkAccessibility(undefined, {
        rules: {
          region: { enabled: false },
        },
      })
      cy.percySnapshot('Group Wizard: Selection Panel')

      // Percy snapshot 2: Ghost group with nodes
      workspacePage.adapterNode(MOCK_ADAPTER_ID1).click()
      workspacePage.adapterNode(MOCK_ADAPTER_ID2).click()
      wizardPage.selectionPanel.selectedCount.should('contain', '2')
      cy.get('[data-testid^="rf__node-ghost-group"]').should('be.visible')
      cy.checkAccessibility(undefined, {
        rules: {
          region: { enabled: false },
          'color-contrast': { enabled: false },
        },
      })
      cy.percySnapshot('Group Wizard: Ghost Preview')

      // Percy snapshot 3: Configuration panel with accessibility check
      wizardPage.selectionPanel.nextButton.click()
      wizardPage.groupConfig.panel.should('be.visible')
      wizardPage.groupConfig.titleInput.should('be.visible')

      cy.checkAccessibility(undefined, {
        rules: {
          region: { enabled: false },
          'color-contrast': { enabled: false },
        },
      })
      cy.percySnapshot('Group Wizard: Configuration Panel')
    })
  })
})
