/**
 * Wizard Progress Bar Tests
 *
 * Cypress component tests for the WizardProgressBar.
 * Following pragmatic testing strategy: only accessibility test is unskipped.
 */

import WizardProgressBar from './WizardProgressBar'
import { useWizardStore } from '@/modules/Workspace/hooks/useWizardStore'
import { EntityType } from './types'

describe('WizardProgressBar', () => {
  beforeEach(() => {
    // Reset wizard store before each test
    const { actions } = useWizardStore.getState()
    actions.cancelWizard()
  })

  // ✅ ACCESSIBILITY TEST - ALWAYS UNSKIPPED
  it('should be accessible', () => {
    // Start a wizard to make the progress bar visible
    const { actions } = useWizardStore.getState()
    actions.startWizard(EntityType.ADAPTER)

    cy.injectAxe()
    cy.mountWithProviders(<WizardProgressBar />)
    cy.checkAccessibility()
  })

  // ⏭️ SKIPPED TESTS - Document expected behavior but skip for rapid development

  it.skip('should not render when wizard is not active', () => {
    cy.mountWithProviders(<WizardProgressBar />)

    cy.getByTestId('wizard-progress-bar').should('not.exist')
  })

  it.skip('should render when wizard is active', () => {
    const { actions } = useWizardStore.getState()
    actions.startWizard(EntityType.ADAPTER)

    cy.mountWithProviders(<WizardProgressBar />)

    cy.getByTestId('wizard-progress-bar').should('be.visible')
  })

  it.skip('should display correct step information for step 1', () => {
    const { actions } = useWizardStore.getState()
    actions.startWizard(EntityType.ADAPTER)

    cy.mountWithProviders(<WizardProgressBar />)

    // Should show "Step 1 of 3" for ADAPTER
    cy.contains('Step 1 of 3').should('be.visible')
  })

  it.skip('should display correct step information after navigation', () => {
    const { actions } = useWizardStore.getState()
    actions.startWizard(EntityType.ADAPTER)
    actions.nextStep()

    cy.mountWithProviders(<WizardProgressBar />)

    // Should show "Step 2 of 3" after next step
    cy.contains('Step 2 of 3').should('be.visible')
  })

  it.skip('should display step description', () => {
    const { actions } = useWizardStore.getState()
    actions.startWizard(EntityType.ADAPTER)

    cy.mountWithProviders(<WizardProgressBar />)

    // Should display the step description from metadata
    cy.contains('Review adapter preview').should('be.visible')
  })

  it.skip('should update step description when navigating', () => {
    const { actions } = useWizardStore.getState()
    actions.startWizard(EntityType.ADAPTER)

    cy.mountWithProviders(<WizardProgressBar />)

    cy.contains('Review adapter preview').should('be.visible')

    // Navigate to next step
    cy.wrap(null).then(() => {
      const { actions } = useWizardStore.getState()
      actions.nextStep()
    })

    cy.contains('Select protocol type').should('be.visible')
  })

  it.skip('should show progress bar with correct percentage', () => {
    const { actions } = useWizardStore.getState()
    actions.startWizard(EntityType.ADAPTER) // 3 steps

    cy.mountWithProviders(<WizardProgressBar />)

    // Step 1 of 3 = ~33%
    cy.get('[role="progressbar"]').should('have.attr', 'aria-valuenow', '33')
  })

  it.skip('should update progress bar when navigating', () => {
    const { actions } = useWizardStore.getState()
    actions.startWizard(EntityType.ADAPTER)
    actions.nextStep()

    cy.mountWithProviders(<WizardProgressBar />)

    // Step 2 of 3 = ~67%
    cy.get('[role="progressbar"]').should('have.attr', 'aria-valuenow', '67')
  })

  it.skip('should display cancel button', () => {
    const { actions } = useWizardStore.getState()
    actions.startWizard(EntityType.ADAPTER)

    cy.mountWithProviders(<WizardProgressBar />)

    cy.getByTestId('wizard-cancel-button').should('be.visible').should('contain', 'Cancel')
  })

  it.skip('should call cancelWizard when cancel button is clicked', () => {
    const { actions } = useWizardStore.getState()
    actions.startWizard(EntityType.ADAPTER)

    cy.mountWithProviders(<WizardProgressBar />)

    // Verify wizard is active
    cy.getByTestId('wizard-progress-bar').should('be.visible')

    // Click cancel
    cy.getByTestId('wizard-cancel-button').click()

    // Verify wizard is no longer active
    cy.getByTestId('wizard-progress-bar').should('not.exist')
  })

  it.skip('should have proper ARIA attributes', () => {
    const { actions } = useWizardStore.getState()
    actions.startWizard(EntityType.ADAPTER)

    cy.mountWithProviders(<WizardProgressBar />)

    // Check region label
    cy.get('[role="region"]').should('have.attr', 'aria-label').and('contain', 'Wizard progress')

    // Check progress bar label
    cy.get('[role="progressbar"]').should('have.attr', 'aria-label')

    // Check cancel button label
    cy.getByTestId('wizard-cancel-button').should('have.attr', 'aria-label')
  })

  it.skip('should be positioned at bottom-center', () => {
    const { actions } = useWizardStore.getState()
    actions.startWizard(EntityType.ADAPTER)

    cy.mountWithProviders(<WizardProgressBar />)

    // Panel should be at bottom-center
    cy.getByTestId('wizard-progress-bar')
      .parent()
      .should('have.class', 'react-flow__panel')
      .should('have.class', 'bottom')
      .should('have.class', 'center')
  })

  it.skip('should work with different wizard types', () => {
    const { actions } = useWizardStore.getState()

    // Test BRIDGE (2 steps)
    actions.startWizard(EntityType.BRIDGE)

    cy.mountWithProviders(<WizardProgressBar />)

    cy.contains('Step 1 of 2').should('be.visible')

    // Test COMBINER (3 steps)
    cy.wrap(null).then(() => {
      const { actions } = useWizardStore.getState()
      actions.cancelWizard()
      actions.startWizard(EntityType.COMBINER)
    })

    cy.contains('Step 1 of 3').should('be.visible')
  })

  it.skip('should handle edge case of last step', () => {
    const { actions } = useWizardStore.getState()
    actions.startWizard(EntityType.BRIDGE) // 2 steps
    actions.nextStep() // Go to step 2

    cy.mountWithProviders(<WizardProgressBar />)

    cy.contains('Step 2 of 2').should('be.visible')

    // Progress should be 100%
    cy.get('[role="progressbar"]').should('have.attr', 'aria-valuenow', '100')
  })

  it.skip('should be responsive on mobile', () => {
    const { actions } = useWizardStore.getState()
    actions.startWizard(EntityType.ADAPTER)

    cy.viewport('iphone-x')
    cy.mountWithProviders(<WizardProgressBar />)

    cy.getByTestId('wizard-progress-bar').should('be.visible')

    // Should take up most of viewport width on mobile
    cy.getByTestId('wizard-progress-bar').find('> div').should('have.css', 'min-width')
  })

  it.skip('should have proper styling', () => {
    const { actions } = useWizardStore.getState()
    actions.startWizard(EntityType.ADAPTER)

    cy.mountWithProviders(<WizardProgressBar />)

    // Should have shadow and rounded corners
    cy.getByTestId('wizard-progress-bar')
      .find('> div')
      .should('have.css', 'box-shadow')
      .should('have.css', 'border-radius')
  })

  it.skip('should display Next button on first step', () => {
    const { actions } = useWizardStore.getState()
    actions.startWizard(EntityType.ADAPTER)

    cy.mountWithProviders(<WizardProgressBar />)

    cy.getByTestId('wizard-next-button').should('be.visible').should('contain', 'Next')
  })

  it.skip('should not display Back button on first step', () => {
    const { actions } = useWizardStore.getState()
    actions.startWizard(EntityType.ADAPTER)

    cy.mountWithProviders(<WizardProgressBar />)

    cy.getByTestId('wizard-back-button').should('not.exist')
  })

  it.skip('should display both Back and Next buttons on middle steps', () => {
    const { actions } = useWizardStore.getState()
    actions.startWizard(EntityType.ADAPTER)
    actions.nextStep() // Move to step 2

    cy.mountWithProviders(<WizardProgressBar />)

    cy.getByTestId('wizard-back-button').should('be.visible').should('contain', 'Back')
    cy.getByTestId('wizard-next-button').should('be.visible').should('contain', 'Next')
  })

  it.skip('should display Complete button on last step', () => {
    const { actions } = useWizardStore.getState()
    actions.startWizard(EntityType.ADAPTER)
    actions.nextStep()
    actions.nextStep() // Move to last step

    cy.mountWithProviders(<WizardProgressBar />)

    cy.getByTestId('wizard-complete-button').should('be.visible').should('contain', 'Complete')
    cy.getByTestId('wizard-next-button').should('not.exist')
  })

  it.skip('should call nextStep when Next button is clicked', () => {
    const { actions } = useWizardStore.getState()
    actions.startWizard(EntityType.ADAPTER)

    cy.mountWithProviders(<WizardProgressBar />)

    cy.getByTestId('wizard-next-button').click()

    // Verify step changed
    cy.contains('Step 2 of 3').should('be.visible')
  })

  it.skip('should call previousStep when Back button is clicked', () => {
    const { actions } = useWizardStore.getState()
    actions.startWizard(EntityType.ADAPTER)
    actions.nextStep()

    cy.mountWithProviders(<WizardProgressBar />)

    cy.getByTestId('wizard-back-button').click()

    // Verify step changed back
    cy.contains('Step 1 of 3').should('be.visible')
  })

  it.skip('should have accessible button labels', () => {
    const { actions } = useWizardStore.getState()
    actions.startWizard(EntityType.ADAPTER)
    actions.nextStep()

    cy.injectAxe()
    cy.mountWithProviders(<WizardProgressBar />)

    // All buttons should have aria-label
    cy.getByTestId('wizard-back-button').should('have.attr', 'aria-label')
    cy.getByTestId('wizard-next-button').should('have.attr', 'aria-label')
    cy.getByTestId('wizard-cancel-button').should('have.attr', 'aria-label')

    cy.checkAccessibility()
  })
})
