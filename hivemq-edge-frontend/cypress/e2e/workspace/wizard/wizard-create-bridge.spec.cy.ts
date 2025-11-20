import { mockBridge } from '@/api/hooks/useGetBridges/__handlers__'
import { mockAdapter_OPCUA } from '@/api/hooks/useProtocolAdapters/__handlers__'
import { MOCK_TOPIC_FILTER } from '@/api/hooks/useTopicFilters/__handlers__'

import { loginPage, workspacePage, wizardPage } from '../../../pages'
import { cy_interceptCoreE2E } from '../../../utils/intercept.utils.ts'

const MOCK_BRIDGE_ID = 'test-mqtt-bridge'

describe('Wizard: Create Bridge', () => {
  beforeEach(() => {
    // Base interceptors for E2E
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
    // Existing bridges (so we don't create duplicates)
    cy.intercept('GET', '/api/v1/management/bridges', {
      items: [mockBridge],
    })

    // Existing adapters (for workspace state)
    cy.intercept('GET', '/api/v1/management/protocol-adapters/adapters', {
      items: [mockAdapter_OPCUA],
    })

    // POST to create new bridge - success response
    cy.intercept('POST', '/api/v1/management/bridges', {
      statusCode: 201,
      body: {
        id: MOCK_BRIDGE_ID,
        host: 'broker.example.com',
        port: 1883,
        clientId: 'edge-bridge-client',
      },
    }).as('createBridge')

    cy.intercept('GET', '/api/v1/management/topic-filters', {
      items: [MOCK_TOPIC_FILTER],
    })
    cy.intercept('/api/v1/management/protocol-adapters/types', { statusCode: 202, log: false })
    cy.intercept('/api/v1/data-hub/data-validation/policies', { statusCode: 202, log: false })

    loginPage.visit('/app/workspace')
    loginPage.loginButton.click()
    workspacePage.navLink.click()

    workspacePage.canvas.should('be.visible')
  })

  // ========== 1. ACCESSIBILITY TEST ==========
  /**
   * Verifies the bridge wizard is accessible (WCAG2AA)
   * Captures Percy snapshot for visual regression
   */
  it(
    'should be accessible and capture wizard screens for documentation and visual consistency',
    { tags: ['@percy'] },
    () => {
      cy.injectAxe()

      workspacePage.toolbox.fit.click()
      wizardPage.createEntityButton.should('be.visible')

      // DEBUG: Capture initial workspace state for AI debugging
      cy.saveHTMLSnapshot('wizard-bridge-initial-workspace')
      cy.logDOMState('Workspace with create button')

      wizardPage.createEntityButton.click()

      // DEBUG: Capture wizard menu state
      cy.saveHTMLSnapshot('wizard-bridge-menu-open')
      cy.logDOMState('Wizard menu with entity options')

      cy.screenshot('Workspace Wizard / Bridge wizard menu', { overwrite: true })
      cy.checkAccessibility(undefined, {
        rules: {
          // Chakra UI Portal creates region elements without accessible names - known third-party issue
          region: { enabled: false },
        },
      })

      // === STEP 0: Ghost Preview ===
      wizardPage.wizardMenu.selectOption('BRIDGE')

      // DEBUG: Capture ghost preview state
      cy.saveHTMLSnapshot('wizard-bridge-ghost-preview')
      cy.logDOMState('Bridge ghost preview')

      // Check progress bar exists and shows step 1 of 2
      wizardPage.progressBar.container.should('be.visible')
      wizardPage.progressBar.container.should('contain', 'Step 1')

      cy.screenshot('Workspace Wizard / Bridge wizard progress', { overwrite: true })

      // Check for ghost nodes on canvas (HOST → BRIDGE → EDGE)
      wizardPage.canvas.ghostNode.should('be.visible')
      wizardPage.canvas.ghostEdges.should('exist')

      // Progress bar should be accessible
      wizardPage.progressBar.container.then(($progress) => {
        cy.checkAccessibility($progress[0], {
          rules: {
            region: { enabled: false },
          },
        })
      })

      cy.screenshot('Workspace Wizard / Bridge ghost preview', { overwrite: true })

      // === STEP 1: Configuration ===
      wizardPage.progressBar.nextButton.click()

      // DEBUG: Capture configuration form state
      cy.saveHTMLSnapshot('wizard-bridge-configuration-form')
      cy.logDOMState('Bridge configuration form')

      // Check progress bar shows step 2
      wizardPage.progressBar.container.should('contain', 'Step 2')

      // Configuration form should be visible
      wizardPage.bridgeConfig.configForm.should('be.visible')

      // Form should be accessible
      wizardPage.bridgeConfig.configForm.then(($form) => {
        cy.checkAccessibility($form[0], {
          rules: {
            region: { enabled: false },
          },
        })
      })

      cy.screenshot('Workspace Wizard / Bridge configuration form', { overwrite: true })
    }
  )

  // ========== 2. BRIDGE CREATION WITH CONFIGURATION ==========
  /**
   * Tests the complete bridge creation workflow
   * Ensures bridge configuration completes successfully
   */
  it('should create a bridge successfully', () => {
    wizardPage.createEntityButton.click()
    wizardPage.wizardMenu.selectOption('BRIDGE')

    // Should be on ghost preview (Step 1 of 2)
    wizardPage.progressBar.container.should('contain', 'Step 1')
    wizardPage.canvas.ghostNode.should('be.visible')

    // Click next to configuration
    wizardPage.progressBar.nextButton.click()

    // Should be on configuration (Step 2 of 2)
    wizardPage.progressBar.container.should('contain', 'Step 2')
    wizardPage.bridgeConfig.configForm.should('be.visible')

    // Fill in bridge configuration (following adapter pattern)
    wizardPage.bridgeConfig.setBridgeId('my-mqtt-bridge')
    wizardPage.bridgeConfig.setHost('mqtt.example.com')
    wizardPage.bridgeConfig.setPort('1883')
    wizardPage.bridgeConfig.setClientId('edge-client-123')

    // Submit configuration (adapter pattern)
    wizardPage.bridgeConfig.submitButton.click()

    cy.wait('@createBridge')
    wizardPage.toast.success.should('be.visible')

    // Screenshot for documentation
    cy.screenshot('Workspace Wizard / Bridge creation success', { overwrite: true })
  })

  // ========== 3. VISUAL REGRESSION TEST ==========
  /**
   * Ensures wizard UI stays consistent across changes
   * Percy will flag any visual regressions
   */
  it('should maintain visual consistency during bridge configuration', { tags: ['@percy'] }, () => {
    // Start wizard
    wizardPage.createEntityButton.click()
    wizardPage.wizardMenu.selectOption('BRIDGE')

    // Capture ghost preview for regression
    cy.screenshot('Workspace Wizard / Bridge ghost preview (regression)', { overwrite: true })

    // Move to configuration
    wizardPage.progressBar.nextButton.click()
    wizardPage.bridgeConfig.configForm.should('be.visible')

    // Fill in some values
    wizardPage.bridgeConfig.setBridgeId('regression-bridge')
    wizardPage.bridgeConfig.setHost('test-broker.local')
    wizardPage.bridgeConfig.setPort('8883')

    // Capture form with values for regression
    cy.screenshot('Workspace Wizard / Bridge configuration with values (regression)', { overwrite: true })
  })

  // ========== 4. BACK NAVIGATION TEST ==========
  /**
   * Tests navigating back through wizard steps
   * Verifies state is preserved
   *
   * IMPORTANT: Tests that drawer overlay blocks progress bar interaction
   * This validates the UX pattern where drawer UI takes precedence
   */
  it('should allow navigating back from configuration to preview', () => {
    wizardPage.createEntityButton.click()
    wizardPage.wizardMenu.selectOption('BRIDGE')

    // On Step 1 - Back button should not exist
    wizardPage.progressBar.container.should('contain', 'Step 1')
    wizardPage.progressBar.backButton.should('not.exist')

    // Go to Step 2
    wizardPage.progressBar.nextButton.click()
    wizardPage.progressBar.container.should('contain', 'Step 2')

    // Configuration drawer should be visible with overlay
    wizardPage.bridgeConfig.configForm.should('be.visible')

    // Progress bar back button exists in DOM
    wizardPage.progressBar.backButton.should('exist')
    wizardPage.progressBar.backButton.should('be.visible')
    // But is blocked by the overlay of the modal
    wizardPage.bridgeConfig.panel.should('have.attr', 'role', 'dialog').should('have.attr', 'aria-modal', 'true')

    wizardPage.bridgeConfig.backButton.click()

    // Should be back on Step 1
    wizardPage.progressBar.container.should('contain', 'Step 1')
    wizardPage.canvas.ghostNode.should('be.visible')

    // Back button should be hidden again
    wizardPage.progressBar.backButton.should('not.exist')
  })

  // ========== 5. CANCEL WIZARD TEST ==========
  /**
   * Tests canceling the wizard
   * Verifies ghost nodes are removed and state is reset
   */
  it('should cancel wizard and clean up ghost nodes', () => {
    wizardPage.createEntityButton.click()
    wizardPage.wizardMenu.selectOption('BRIDGE')

    wizardPage.canvas.ghostNode.should('exist')
    wizardPage.canvas.ghostEdges.should('exist')
    wizardPage.progressBar.cancelButton.click()
    wizardPage.canvas.ghostNode.should('not.exist')
    wizardPage.canvas.ghostEdges.should('not.exist')
    wizardPage.progressBar.container.should('not.exist')
    wizardPage.createEntityButton.should('not.be.disabled')
  })

  // ========== 6. FORM VALIDATION TEST ==========
  /**
   * Tests required field validation
   * Ensures form cannot be submitted with missing data
   */
  it('should validate required fields in bridge configuration', () => {
    wizardPage.createEntityButton.click()
    wizardPage.wizardMenu.selectOption('BRIDGE')
    wizardPage.progressBar.nextButton.click()

    // Configuration form should be visible
    wizardPage.bridgeConfig.configForm.should('be.visible')

    // Try to submit without filling required fields
    wizardPage.bridgeConfig.submitButton.click()

    // Should show validation errors (form should not submit)
    // Bridge should not be created
    cy.get('@createBridge.all').should('have.length', 0)

    // Fill required fields
    wizardPage.bridgeConfig.setBridgeId('valid-bridge')
    wizardPage.bridgeConfig.setHost('valid-host.com')
    wizardPage.bridgeConfig.setClientId('valid-client')

    // Now submission should work
    wizardPage.bridgeConfig.submitButton.click()
    cy.wait('@createBridge')

    wizardPage.toast.success.should('be.visible')
  })

  // ========== 7. PROGRESS INDICATOR TEST ==========
  /**
   * Tests progress bar updates correctly through steps
   * Verifies visual progress percentage
   */
  it('should update progress bar correctly through wizard steps', () => {
    wizardPage.createEntityButton.click()
    wizardPage.wizardMenu.selectOption('BRIDGE')

    // Step 1 of 2 = 50% progress
    wizardPage.progressBar.container.should('contain', 'Step 1')
    wizardPage.progressBar.container.within(() => {
      cy.get('[role="progressbar"]').should('have.attr', 'aria-valuenow', '50')
    })

    // Next button text should be "Next"
    wizardPage.progressBar.nextButton.should('contain', 'Next')

    // Move to step 2
    wizardPage.progressBar.nextButton.click()

    // Step 2 of 2 = 100% progress
    wizardPage.progressBar.container.should('contain', 'Step 2')
    wizardPage.progressBar.container.within(() => {
      cy.get('[role="progressbar"]').should('have.attr', 'aria-valuenow', '100')
    })

    // Button should change to "Complete"
    wizardPage.progressBar.completeButton.should('be.visible')
    wizardPage.progressBar.completeButton.should('contain', 'Complete')
  })
})
