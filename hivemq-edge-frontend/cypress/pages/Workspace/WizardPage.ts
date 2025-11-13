/**
 * Wizard Page Object
 *
 * Provides getters for accessing wizard UI elements
 * CRITICAL: Selection getters required for testing wizard selection steps
 */

import type { MockAdapterType } from '@/__test-utils__/adapters/types.ts'
import { Page } from '../Page.ts'

export class WizardPage extends Page {
  get createEntityButton() {
    return cy.getByTestId('create-entity-button')
  }

  /**
   * ========== WIZARD MENU OPTIONS ==========
   */
  wizardMenu = {
    selectOption(type: 'ADAPTER' | 'BRIDGE' | 'COMBINER' | 'ASSET_MAPPER') {
      return cy.getByTestId(`wizard-option-${type}`).click()
    },
  }

  /**
   * ========== WIZARD PROGRESS BAR ==========
   */
  progressBar = {
    get container() {
      return cy.getByTestId('wizard-progress-bar')
    },

    get currentStep() {
      return this.container.within(() => {
        cy.get('[role="progressbar"]')
      })
    },

    get nextButton() {
      return cy.getByTestId('wizard-next-button')
    },
  }

  /**
   * ========== WIZARD CANVAS (Selection Restrictions) ==========
   * Canvas represents the workspace where:
   * - Selectable nodes are visible and enabled
   * - Non-selectable nodes are hidden or disabled
   * - Ghost nodes appear for preview
   * - Ghost edges show connections
   */
  canvas = {
    get container() {
      return cy.getByTestId('rf__wrapper')
    },

    // Get node by its ID on canvas
    node(nodeId: string) {
      return this.container.within(() => {
        cy.get(`[data-id="${nodeId}"]`)
      })
    },

    // Check if node is selectable (visible, enabled, clickable)
    nodeIsSelectable(nodeId: string) {
      return this.node(nodeId).should('be.visible').should('not.have.attr', 'data-disabled', 'true')
    },

    // Check if node is hidden/restricted
    nodeIsRestricted(nodeId: string) {
      return this.node(nodeId)
        .should('have.attr', 'data-disabled', 'true')
        .or(this.node(nodeId).should('have.css', 'opacity', '0.5'))
    },

    // Select a node (clicks it)
    selectNode(nodeId: string) {
      return this.node(nodeId).click()
    },

    // Get ghost node (preview of what will be created)
    get ghostNode() {
      return this.container.within(() => {
        cy.get('[data-testid^="rf__node-ghost-"]')
      })
    },

    // Get ghost edges (preview of connections)
    get ghostEdges() {
      return this.container.within(() => {
        cy.get('[data-testid^="rf__edge-ghost-"]')
      })
    },
  }

  // ========== ADAPTER CONFIGURATION ==========
  adapterConfig = {
    get panel() {
      return cy.getByTestId('wizard-configuration-panel')
    },

    get submitButton() {
      return this.panel.find('button[type="submit"]').first()
    },

    // Protocol selector dropdown
    get protocolSelectors() {
      // return this.panel.within(() => {
      return cy.get('[role="list"]')
      // })
    },

    // Protocol selector dropdown
    protocolSelector(protocolName: MockAdapterType) {
      // return this.protocolSelectors.within(() => {
      return cy.get(`[role="listitem"][aria-labelledby="adapter-${protocolName}"]`)
      // })
    },

    selectProtocol(protocolName: MockAdapterType) {
      return cy
        .get(`[role="listitem"][aria-labelledby="adapter-${protocolName}"]`)
        .findByTestId('protocol-create-adapter')
    },

    get adapterNameInput() {
      return cy.get('[data-testid="root_id"] input[name="root_id"]')
    },

    setAdapterName(name: string) {
      return this.adapterNameInput.clear().type(name)
    },

    // Protocol-specific configuration form
    get configForm() {
      return this.panel.find('form#wizard-adapter-form')
    },
  }

  // ========== BRIDGE CONFIGURATION ==========
  bridgeConfig = {
    get panel() {
      return cy.getByTestId('wizard-configuration-panel')
    },

    // Protocol-specific configuration form
    get configForm() {
      return this.panel.find('form#wizard-bridge-form')
    },

    get bridgeNameInput() {
      return this.configForm.find('[data-testid="root_id"] input[name="root_id"]')
    },

    get bridgeHostInput() {
      return this.configForm.find('[data-testid="root_host"] input[name="root_host"]')
    },

    get bridgeClientIdInput() {
      return this.configForm.find('[data-testid="root_clientId"] input[name="root_clientId"]')
    },
  }

  /**
   * ========== WIZARD SELECTION PANEL (CRITICAL) ==========
   * WizardSelectionPanel provides:
   * - Selected nodes count
   * - List of selected nodes
   * - Validation messages (min/max constraints)
   * - Next button (enabled/disabled based on constraints)
   * - Close button (cancel wizard)
   */
  selectionPanel = {
    // Panel container
    get panel() {
      return cy.get('[data-testid="wizard-selection-panel"], [data-testid="wizard-progress-panel"]').first()
    },

    // Selected nodes count - CRITICAL for testing constraints
    get selectedCount() {
      return this.panel.within(() => {
        cy.get('[data-testid="selection-count"]')
      })
    },

    // List of selected node items
    get selectedNodesList() {
      return this.panel.within(() => {
        cy.get('[data-testid="selected-nodes-list"], ul, [role="list"]').first()
      })
    },

    // Get specific selected node by ID
    selectedNode(nodeId: string) {
      return this.selectedNodesList.within(() => {
        cy.contains(nodeId).parent()
      })
    },

    // Remove button on selected node
    removeSelectedNode(nodeId: string) {
      return this.selectedNode(nodeId).within(() => {
        cy.get('button, [role="button"]').filter((_, el) => {
          return el.textContent?.includes('×') || el.getAttribute('aria-label')?.includes('remove')
        })
      })
    },

    // Validation message (min/max constraints)
    get validationMessage() {
      return this.panel.within(() => {
        cy.get('[data-testid="selection-validation"]')
      })
    },

    // Next button - CRITICAL state tracking
    get nextButton() {
      return this.panel.within(() => {
        cy.get('button, [role="button"]').filter((_, el) => {
          return (
            el.textContent?.includes('Next') ||
            el.textContent?.includes('Continue') ||
            el.textContent?.includes('Proceed')
          )
        })
      })
    },

    // Check if next button is enabled
    isNextButtonEnabled() {
      return this.nextButton.should('not.be.disabled')
    },

    // Close/Cancel button
    get closeButton() {
      return this.panel.within(() => {
        cy.get('button, [role="button"]')
          .filter((_, el) => {
            return el.textContent?.includes('×') || el.getAttribute('aria-label')?.includes('close')
          })
          .first()
      })
    },
  }

  // ========== COMBINER CONFIGURATION ==========
  combinerConfig = {
    get configForm() {
      return cy.get('form[data-testid="combiner-config-form"]')
    },

    getMappingInput(index: number) {
      return this.configForm.within(() => {
        cy.get('input[placeholder*="mapping" i]').eq(index)
      })
    },

    setMappingInput(index: number, value: string) {
      return this.getMappingInput(index).clear().type(value)
    },
  }

  // ========== WIZARD COMPLETION ==========
  completion = {
    get successMessage() {
      return cy.getByTestId('wizard-success-message')
    },

    get closeWizardButton() {
      return cy.getByTestId('wizard-close-button')
    },

    entityAppearsOnCanvas(entityId: string) {
      return cy.getByTestId('rf__wrapper').within(() => {
        cy.contains(entityId).should('be.visible')
      })
    },
  }

  // ========== UTILITY METHODS ==========

  /**
   * Get current selected count from panel
   */
  getSelectedCount = (): Cypress.Chainable<string> => {
    return this.selectionPanel.selectedCount.invoke('text').then((text) => {
      // Extract number from text like "2 selected" or "2 / 5"
      const match = text.match(/(\d+)/)
      return match ? match[1] : '0'
    })
  }

  /**
   * Verify n nodes are selected
   */
  verifyNodesSelected = (count: number) => {
    this.getSelectedCount().should('equal', count.toString())
  }

  toast = {
    get success() {
      return cy.get('[role="region"] [role="status"] [data-status="success"]')
    },

    get error() {
      return cy.get('[role="region"] [role="status"] > [data-status="error"]')
    },

    close() {
      cy.get('[role="status"]').within(() => {
        cy.getByAriaLabel('Close').click()
      })
    },
  }
}

export const wizardPage = new WizardPage()
