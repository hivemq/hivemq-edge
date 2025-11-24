import { ReactFlowTesting } from '@/__test-utils__/react-flow/ReactFlowTesting'
import { useWizardStore } from '@/modules/Workspace/hooks/useWizardStore'
import { EntityType } from './types'
import WizardSelectionPanel from './WizardSelectionPanel'
import type { NodeAdapterType, NodeBridgeType, NodeDeviceType, NodePulseType } from '@/modules/Workspace/types'
import { NodeTypes } from '@/modules/Workspace/types'
import type { Node, Edge } from '@xyflow/react'
import { mockAdapter } from '@/api/hooks/useProtocolAdapters/__handlers__'
import { mockBridge } from '@/api/hooks/useGetBridges/__handlers__'
import { mockProtocolAdapter } from '@/api/hooks/useProtocolAdapters/__handlers__'
import { PulseStatus } from '@/api/__generated__'

describe('WizardSelectionPanel', () => {
  beforeEach(() => {
    // Reset wizard store before each test
    const { actions } = useWizardStore.getState()
    actions.cancelWizard()
  })

  // ✅ Properly typed mock nodes using actual API data structures
  const mockAdapterNode: NodeAdapterType = {
    id: 'adapter-1',
    type: NodeTypes.ADAPTER_NODE,
    position: { x: 100, y: 100 },
    data: {
      ...mockAdapter,
      id: 'adapter-1',
    },
  }

  const mockBridgeNode: NodeBridgeType = {
    id: 'bridge-1',
    type: NodeTypes.BRIDGE_NODE,
    position: { x: 300, y: 100 },
    data: {
      ...mockBridge,
      id: 'bridge-1',
    },
  }

  const mockPulseNode: NodePulseType = {
    id: 'pulse-1',
    type: NodeTypes.PULSE_NODE,
    position: { x: 500, y: 100 },
    data: {
      id: 'pulse-1',
      label: 'Pulse Agent',
      status: {
        activation: PulseStatus.activation.ACTIVATED,
        runtime: PulseStatus.runtime.CONNECTED,
      },
    },
  }

  const mockDeviceNode: NodeDeviceType = {
    id: 'device-1',
    type: NodeTypes.DEVICE_NODE,
    position: { x: 150, y: 200 },
    data: {
      ...mockProtocolAdapter,
      sourceAdapterId: 'adapter-1',
    },
  }

  const mountComponent = (nodes: Node[] = [], edges: Edge[] = []) => {
    const Wrapper: React.JSXElementConstructor<{ children: React.ReactNode }> = ({ children }) => (
      <ReactFlowTesting
        config={{
          initialState: {
            nodes,
            edges,
          },
        }}
      >
        {children}
      </ReactFlowTesting>
    )
    return cy.mountWithProviders(<WizardSelectionPanel />, { wrapper: Wrapper })
  }

  describe('accessibility', () => {
    it('should be accessible', () => {
      mountComponent([mockAdapterNode, mockBridgeNode])

      cy.then(() => {
        const { actions } = useWizardStore.getState()
        actions.startWizard(EntityType.GROUP)
        actions.selectNode('adapter-1')
        actions.selectNode('bridge-1')
      })

      cy.injectAxe()

      cy.checkAccessibility(undefined, {
        rules: {
          'color-contrast': { enabled: false },
        },
      })
    })

    it('should have proper ARIA labels for buttons', () => {
      mountComponent([mockAdapterNode])

      cy.then(() => {
        const { actions } = useWizardStore.getState()
        actions.startWizard(EntityType.GROUP)
        actions.selectNode('adapter-1')
      })

      cy.get('button[aria-label="Close"]').should('exist')
      cy.getByTestId('wizard-selection-listItem-adapter-1').within(() => {
        cy.get('button').should('have.attr', 'aria-label', 'Remove from selection')
      })
    })
  })

  describe('visibility', () => {
    it('should not render when wizard is not active', () => {
      mountComponent()

      cy.getByTestId('wizard-selection-panel').should('not.exist')
    })

    it('should not render when selectionConstraints is null', () => {
      mountComponent()

      cy.then(() => {
        // Start wizard but ensure no selection constraints
        const { actions } = useWizardStore.getState()
        actions.startWizard(EntityType.ADAPTER)
        useWizardStore.setState({ selectionConstraints: null })
      })

      cy.getByTestId('wizard-selection-panel').should('not.exist')
    })

    it('should render when wizard is active with selection constraints', () => {
      mountComponent()

      cy.then(() => {
        const { actions } = useWizardStore.getState()
        actions.startWizard(EntityType.GROUP)
      })

      cy.getByTestId('wizard-selection-panel').should('be.visible')
    })
  })

  describe('header and close button', () => {
    it('should display title and description', () => {
      mountComponent()

      cy.then(() => {
        const { actions } = useWizardStore.getState()
        actions.startWizard(EntityType.GROUP)
      })

      cy.getByTestId('wizard-selection-panel').within(() => {
        cy.contains(/select nodes/i).should('be.visible')
      })
    })

    it('should have close button that cancels wizard', () => {
      mountComponent()

      cy.then(() => {
        const { actions } = useWizardStore.getState()
        actions.startWizard(EntityType.GROUP)
      })

      // Click close button
      cy.getByTestId('wizard-selection-panel').within(() => {
        cy.get('button[aria-label="Close"]').click()
      })

      // Verify wizard was cancelled
      cy.wrap(null).then(() => {
        expect(useWizardStore.getState().isActive).to.be.false
      })
    })
  })

  describe('selection count and progress', () => {
    it('should show 0 selection count initially', () => {
      mountComponent()

      cy.then(() => {
        const { actions } = useWizardStore.getState()
        actions.startWizard(EntityType.GROUP)
      })

      cy.getByTestId('wizard-selection-count').should('contain', '0')
    })

    it('should update selection count when nodes are selected', () => {
      mountComponent([mockAdapterNode])

      cy.then(() => {
        const { actions } = useWizardStore.getState()
        actions.startWizard(EntityType.GROUP)
        actions.selectNode('adapter-1')
      })

      cy.getByTestId('wizard-selection-count').should('contain', '1')
    })

    it('should show valid status when minimum requirement is met', () => {
      mountComponent([
        mockAdapterNode,
        { ...mockAdapterNode, id: 'adapter-2', data: { ...mockAdapter, id: 'adapter-2' } },
      ])

      cy.then(() => {
        const { actions } = useWizardStore.getState()
        actions.startWizard(EntityType.GROUP)
        actions.selectNode('adapter-1')
        actions.selectNode('adapter-2')
      })

      cy.getByTestId('wizard-selection-count').should('have.attr', 'data-status', 'valid')
    })

    it('should show incomplete status when below minimum requirement', () => {
      mountComponent([mockAdapterNode])

      cy.then(() => {
        const { actions } = useWizardStore.getState()
        actions.startWizard(EntityType.GROUP)
        actions.selectNode('adapter-1')
      })

      cy.getByTestId('wizard-selection-count').should('have.attr', 'data-status', 'incomplete')
    })
  })

  describe('selected nodes list', () => {
    it('should show info message when no nodes are selected', () => {
      mountComponent()

      cy.then(() => {
        const { actions } = useWizardStore.getState()
        actions.startWizard(EntityType.GROUP)
      })

      cy.getByTestId('wizard-selection-panel').within(() => {
        cy.get('[role="alert"]').should('contain', 'Click nodes on the canvas to select them')
      })
    })

    it('should render list of selected nodes', () => {
      mountComponent([mockAdapterNode])

      cy.then(() => {
        const { actions } = useWizardStore.getState()
        actions.startWizard(EntityType.GROUP)
        actions.selectNode('adapter-1')
      })

      cy.getByTestId('wizard-selection-list').should('be.visible')
      cy.getByTestId('wizard-selection-listItem-adapter-1').should('be.visible')
      // The mockAdapter data has id as its identifier
      cy.contains('adapter-1').should('be.visible')
    })

    it('should show node type for each selected node', () => {
      mountComponent([mockAdapterNode])

      cy.then(() => {
        const { actions } = useWizardStore.getState()
        actions.startWizard(EntityType.GROUP)
        actions.selectNode('adapter-1')
      })

      cy.getByTestId('wizard-selection-listItem-adapter-1').within(() => {
        cy.contains('ADAPTER').should('be.visible')
      })
    })

    it('should render multiple selected nodes', () => {
      mountComponent([mockAdapterNode, mockBridgeNode])

      cy.then(() => {
        const { actions } = useWizardStore.getState()
        actions.startWizard(EntityType.GROUP)
        actions.selectNode('adapter-1')
        actions.selectNode('bridge-1')
      })

      cy.getByTestId('wizard-selection-listItem-adapter-1').should('be.visible')
      cy.getByTestId('wizard-selection-listItem-bridge-1').should('be.visible')
    })

    it('should have remove button for each node', () => {
      mountComponent([mockAdapterNode])

      cy.then(() => {
        const { actions } = useWizardStore.getState()
        actions.startWizard(EntityType.GROUP)
        actions.selectNode('adapter-1')
      })

      cy.getByTestId('wizard-selection-listItem-adapter-1').within(() => {
        cy.get('button').should('have.attr', 'aria-label', 'Remove from selection')
      })
    })

    it('should remove node when remove button is clicked', () => {
      mountComponent([mockAdapterNode, mockBridgeNode])

      cy.then(() => {
        const { actions } = useWizardStore.getState()
        actions.startWizard(EntityType.GROUP)
        actions.selectNode('adapter-1')
        actions.selectNode('bridge-1')
      })

      // Remove first node
      cy.getByTestId('wizard-selection-listItem-adapter-1').within(() => {
        cy.get('button').click()
      })

      // Verify node was removed from store
      cy.wrap(null).then(() => {
        const state = useWizardStore.getState()
        expect(state.selectedNodeIds).to.not.include('adapter-1')
        expect(state.selectedNodeIds).to.include('bridge-1')
      })
    })
  })

  describe('Asset Mapper special handling', () => {
    it('should show Pulse Agent required message for Asset Mapper', () => {
      mountComponent([mockPulseNode])

      cy.then(() => {
        const { actions } = useWizardStore.getState()
        actions.startWizard(EntityType.ASSET_MAPPER)
      })

      cy.contains(/pulse agent.*required/i).should('be.visible')
    })

    it('should mark Pulse Agent as required in Asset Mapper', () => {
      mountComponent([mockPulseNode])

      cy.then(() => {
        const { actions } = useWizardStore.getState()
        actions.startWizard(EntityType.ASSET_MAPPER)
        actions.selectNode('pulse-1')
      })

      cy.getByTestId('wizard-selection-listItem-pulse-1').within(() => {
        cy.contains('Required').should('be.visible')
      })
    })

    it('should disable remove button for Pulse Agent in Asset Mapper', () => {
      mountComponent([mockPulseNode])

      cy.then(() => {
        const { actions } = useWizardStore.getState()
        actions.startWizard(EntityType.ASSET_MAPPER)
        actions.selectNode('pulse-1')
      })

      cy.getByTestId('wizard-selection-listItem-pulse-1').within(() => {
        cy.get('button').should('be.disabled')
      })
    })

    it('should allow removing non-Pulse nodes in Asset Mapper', () => {
      mountComponent([mockPulseNode, mockAdapterNode])

      cy.then(() => {
        const { actions } = useWizardStore.getState()
        actions.startWizard(EntityType.ASSET_MAPPER)
        actions.selectNode('pulse-1')
        actions.selectNode('adapter-1')
      })

      cy.getByTestId('wizard-selection-listItem-adapter-1').within(() => {
        cy.get('button').should('not.be.disabled')
      })
    })
  })

  describe('Group wizard auto-included nodes', () => {
    it('should show auto-included count for GROUP wizard', () => {
      // Create edge between adapter and device for auto-inclusion logic
      const adapterDeviceEdge = {
        id: 'edge-adapter-device',
        source: 'adapter-1',
        target: 'device-1',
      }

      mountComponent([mockAdapterNode, mockDeviceNode], [adapterDeviceEdge])

      cy.then(() => {
        const { actions } = useWizardStore.getState()
        actions.startWizard(EntityType.GROUP)
        actions.selectNode('adapter-1')
      })

      // Translation: "1 node selected (1 auto-included)"
      cy.getByTestId('wizard-selection-count').should('contain', '1').and('contain', 'auto-included')
    })
  })

  describe('validation messages', () => {
    it('should show minimum warning when below threshold', () => {
      mountComponent([mockAdapterNode])

      cy.then(() => {
        const { actions } = useWizardStore.getState()
        actions.startWizard(EntityType.GROUP) // Requires minimum 2 nodes
        actions.selectNode('adapter-1')
      })

      cy.getByTestId('wizard-selection-validation').should('be.visible')
      cy.contains(/select.*1.*more/i).should('be.visible')
    })

    it('should not show minimum warning when threshold is met', () => {
      mountComponent([mockAdapterNode, mockBridgeNode])

      cy.then(() => {
        const { actions } = useWizardStore.getState()
        actions.startWizard(EntityType.GROUP)
        actions.selectNode('adapter-1')
        actions.selectNode('bridge-1')
      })

      cy.getByTestId('wizard-selection-validation').should('not.exist')
    })

    it('should show allowed node types info when no selection', () => {
      mountComponent()

      cy.then(() => {
        const { actions } = useWizardStore.getState()
        actions.startWizard(EntityType.GROUP)
      })

      cy.getByTestId('wizard-selection-panel').within(() => {
        cy.get('[role="alert"]').should('contain', 'You can select:')
      })
    })

    it('should hide allowed node types info after selection', () => {
      mountComponent([mockAdapterNode])

      cy.then(() => {
        const { actions } = useWizardStore.getState()
        actions.startWizard(EntityType.GROUP)
        actions.selectNode('adapter-1')
      })

      cy.getByTestId('wizard-selection-panel').within(() => {
        cy.get('[role="alert"]').should('not.contain', 'You can select:')
      })
    })
  })

  describe('next button', () => {
    it('should be disabled when minimum requirement not met', () => {
      mountComponent([mockAdapterNode])

      cy.then(() => {
        const { actions } = useWizardStore.getState()
        actions.startWizard(EntityType.GROUP)
        actions.selectNode('adapter-1')
      })

      cy.getByTestId('wizard-selection-next').should('be.disabled')
    })

    it('should be enabled when minimum requirement is met', () => {
      mountComponent([mockAdapterNode, mockBridgeNode])

      cy.then(() => {
        const { actions } = useWizardStore.getState()
        actions.startWizard(EntityType.GROUP)
        actions.selectNode('adapter-1')
        actions.selectNode('bridge-1')
      })

      cy.getByTestId('wizard-selection-next').should('not.be.disabled')
    })

    it('should call nextStep when clicked', () => {
      mountComponent([mockAdapterNode, mockBridgeNode])

      let initialStep: number

      cy.then(() => {
        const { actions } = useWizardStore.getState()
        actions.startWizard(EntityType.GROUP)
        actions.selectNode('adapter-1')
        actions.selectNode('bridge-1')
        initialStep = useWizardStore.getState().currentStep
      })

      cy.getByTestId('wizard-selection-next').click()

      cy.wrap(null).then(() => {
        expect(useWizardStore.getState().currentStep).to.equal(initialStep + 1)
      })
    })

    it('should have tooltip when disabled', () => {
      mountComponent([mockAdapterNode])

      cy.then(() => {
        const { actions } = useWizardStore.getState()
        actions.startWizard(EntityType.GROUP)
        actions.selectNode('adapter-1')
      })

      cy.getByTestId('wizard-selection-next').should('have.attr', 'title')
    })
  })

  describe('fallback node label', () => {
    it('should use node ID when label is not available', () => {
      const nodeWithoutLabel: Node = {
        id: 'adapter-no-label',
        type: NodeTypes.ADAPTER_NODE,
        position: { x: 100, y: 100 },
        data: {
          id: 'adapter-no-label',
          // No label property
        },
      }

      mountComponent([nodeWithoutLabel])

      cy.then(() => {
        const { actions } = useWizardStore.getState()
        actions.startWizard(EntityType.GROUP)
        actions.selectNode('adapter-no-label')
      })

      cy.contains('adapter-no-label').should('be.visible')
    })
  })
})
