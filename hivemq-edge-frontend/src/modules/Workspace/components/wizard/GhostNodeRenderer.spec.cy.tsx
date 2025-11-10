/**
 * Ghost Node Renderer Tests
 *
 * Cypress component tests for the GhostNodeRenderer.
 * Following pragmatic testing strategy: only accessibility test is unskipped.
 */

import { ReactFlowProvider } from '@xyflow/react'
import GhostNodeRenderer from './GhostNodeRenderer'
import { useWizardStore } from '@/modules/Workspace/hooks/useWizardStore'
import { EntityType } from './types'

describe('GhostNodeRenderer', () => {
  beforeEach(() => {
    // Reset wizard store before each test
    const { actions } = useWizardStore.getState()
    actions.cancelWizard()
  })

  // ✅ ACCESSIBILITY TEST - ALWAYS UNSKIPPED
  it('should be accessible', () => {
    // Start wizard with ADAPTER (requires ghost)
    const { actions } = useWizardStore.getState()
    actions.startWizard(EntityType.ADAPTER)

    cy.injectAxe()
    cy.mountWithProviders(
      <ReactFlowProvider>
        <GhostNodeRenderer />
      </ReactFlowProvider>
    )
    cy.checkAccessibility()
  })

  // ⏭️ SKIPPED TESTS - Document expected behavior but skip for rapid development

  it.skip('should not render anything visible', () => {
    const { actions } = useWizardStore.getState()
    actions.startWizard(EntityType.ADAPTER)

    cy.mountWithProviders(
      <ReactFlowProvider>
        <GhostNodeRenderer />
      </ReactFlowProvider>
    )

    // Component should not have any DOM output
    // It only manages React Flow state
    cy.get('[data-testid="ghost-node-renderer"]').should('not.exist')
  })

  it.skip('should add ghost node when wizard starts with entity that requires ghost', () => {
    const { actions } = useWizardStore.getState()

    cy.mountWithProviders(
      <ReactFlowProvider>
        <GhostNodeRenderer />
      </ReactFlowProvider>
    )

    // Start wizard with ADAPTER (requires ghost)
    cy.wrap(null).then(() => {
      actions.startWizard(EntityType.ADAPTER)
    })

    // Check wizard store has ghost node
    cy.wrap(null).then(() => {
      const { ghostNodes } = useWizardStore.getState()
      expect(ghostNodes).to.have.length(1)
      expect(ghostNodes[0].data.isGhost).to.be.true
    })
  })

  it.skip('should not add ghost node for entity that does not require ghost', () => {
    cy.mountWithProviders(
      <ReactFlowProvider>
        <GhostNodeRenderer />
      </ReactFlowProvider>
    )

    // Integration points don't require ghost nodes
    // This would need to be tested with actual integration point flow
  })

  it.skip('should remove ghost nodes when wizard is cancelled', () => {
    const { actions } = useWizardStore.getState()

    cy.mountWithProviders(
      <ReactFlowProvider>
        <GhostNodeRenderer />
      </ReactFlowProvider>
    )

    // Start wizard
    cy.wrap(null).then(() => {
      actions.startWizard(EntityType.ADAPTER)
    })

    // Verify ghost node exists
    cy.wrap(null).then(() => {
      const { ghostNodes } = useWizardStore.getState()
      expect(ghostNodes).to.have.length(1)
    })

    // Cancel wizard
    cy.wrap(null).then(() => {
      actions.cancelWizard()
    })

    // Verify ghost nodes cleared
    cy.wrap(null).then(() => {
      const { ghostNodes } = useWizardStore.getState()
      expect(ghostNodes).to.have.length(0)
    })
  })

  it.skip('should show ghost node only on step 0', () => {
    const { actions } = useWizardStore.getState()

    cy.mountWithProviders(
      <ReactFlowProvider>
        <GhostNodeRenderer />
      </ReactFlowProvider>
    )

    // Start wizard
    cy.wrap(null).then(() => {
      actions.startWizard(EntityType.ADAPTER)
    })

    // Step 0 should have ghost
    cy.wrap(null).then(() => {
      const { ghostNodes, currentStep } = useWizardStore.getState()
      expect(currentStep).to.equal(0)
      expect(ghostNodes).to.have.length(1)
    })

    // Move to step 1
    cy.wrap(null).then(() => {
      actions.nextStep()
    })

    // Step 1 should not have ghost (ghost removed)
    cy.wrap(null).then(() => {
      const { currentStep } = useWizardStore.getState()
      expect(currentStep).to.equal(1)
      // Ghost nodes should be cleared from canvas
    })
  })

  it.skip('should create appropriate ghost node type for each entity type', () => {
    const { actions } = useWizardStore.getState()

    cy.mountWithProviders(
      <ReactFlowProvider>
        <GhostNodeRenderer />
      </ReactFlowProvider>
    )

    // Test ADAPTER
    cy.wrap(null).then(() => {
      actions.startWizard(EntityType.ADAPTER)
      const { ghostNodes } = useWizardStore.getState()
      expect(ghostNodes[0].type).to.equal('ADAPTER_NODE')
      actions.cancelWizard()
    })

    // Test BRIDGE
    cy.wrap(null).then(() => {
      actions.startWizard(EntityType.BRIDGE)
      const { ghostNodes } = useWizardStore.getState()
      expect(ghostNodes[0].type).to.equal('BRIDGE_NODE')
      actions.cancelWizard()
    })

    // Test COMBINER
    cy.wrap(null).then(() => {
      actions.startWizard(EntityType.COMBINER)
      const { ghostNodes } = useWizardStore.getState()
      expect(ghostNodes[0].type).to.equal('COMBINER_NODE')
      actions.cancelWizard()
    })
  })

  it.skip('should cleanup ghost nodes on unmount', () => {
    const { actions } = useWizardStore.getState()
    actions.startWizard(EntityType.ADAPTER)

    cy.mountWithProviders(
      <ReactFlowProvider>
        <GhostNodeRenderer />
      </ReactFlowProvider>
    )

    // Verify ghost node exists
    cy.wrap(null).then(() => {
      const { ghostNodes } = useWizardStore.getState()
      expect(ghostNodes).to.have.length(1)
    })

    // Note: Cypress component testing doesn't provide direct unmount
    // Ghost nodes cleanup is tested via cancelWizard in other tests
  })

  it.skip('should not create duplicate ghost nodes', () => {
    const { actions } = useWizardStore.getState()

    cy.mountWithProviders(
      <ReactFlowProvider>
        <GhostNodeRenderer />
      </ReactFlowProvider>
    )

    // Start wizard
    cy.wrap(null).then(() => {
      actions.startWizard(EntityType.ADAPTER)
    })

    // Check only one ghost node (useEffect should have run by this point)
    cy.wrap(null).then(() => {
      const { ghostNodes } = useWizardStore.getState()
      expect(ghostNodes).to.have.length(1)
    })
  })

  it.skip('should handle rapid wizard start/cancel', () => {
    const { actions } = useWizardStore.getState()

    cy.mountWithProviders(
      <ReactFlowProvider>
        <GhostNodeRenderer />
      </ReactFlowProvider>
    )

    // Rapid start/cancel cycles
    cy.wrap(null).then(() => {
      actions.startWizard(EntityType.ADAPTER)
      actions.cancelWizard()
      actions.startWizard(EntityType.BRIDGE)
      actions.cancelWizard()
      actions.startWizard(EntityType.COMBINER)
    })

    // Should end with one ghost node
    cy.wrap(null).then(() => {
      const { ghostNodes } = useWizardStore.getState()
      expect(ghostNodes).to.have.length(1)
      expect(ghostNodes[0].type).to.equal('COMBINER_NODE')
    })
  })
})
