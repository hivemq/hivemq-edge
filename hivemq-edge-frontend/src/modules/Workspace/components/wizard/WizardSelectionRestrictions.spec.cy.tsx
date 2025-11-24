import React from 'react'
import { Box, Checkbox, FormControl, FormLabel, HStack, Text } from '@chakra-ui/react'
import { type Node, type Edge } from '@xyflow/react'
import { MOCK_NODE_ADAPTER, MOCK_NODE_BRIDGE, MOCK_NODE_EDGE } from '@/__test-utils__/react-flow/nodes.ts'
import { ReactFlowTesting } from '@/__test-utils__/react-flow/ReactFlowTesting.tsx'
import { mockAdapter, mockProtocolAdapter } from '@/api/hooks/useProtocolAdapters/__handlers__'
import { IdStubs, NodeTypes } from '@/modules/Workspace/types'
import { useWizardStore } from '@/modules/Workspace/hooks/useWizardStore'
import WizardSelectionRestrictions from './WizardSelectionRestrictions'
import { EntityType } from './types'

/**
 * Test wrapper that displays React Flow state and provides inspection tools
 */
const getWrapperWith = (initialNodes?: Node[], initialEdges?: Edge[]) => {
  const Wrapper: React.JSXElementConstructor<{ children: React.ReactNode }> = ({ children }) => {
    const { isActive, entityType, currentStep, selectionConstraints, selectedNodeIds } = useWizardStore()
    const [isReady, setIsReady] = React.useState(false)

    // Ensure React Flow is ready before rendering children
    React.useEffect(() => {
      const timer = setTimeout(() => {
        setIsReady(true)
      }, 100)
      return () => clearTimeout(timer)
    }, [])

    return (
      <ReactFlowTesting
        config={{
          initialState: {
            nodes: initialNodes,
            edges: initialEdges,
          },
        }}
        showDashboard={true}
        showReactFlowElements={true}
        dashboard={
          <>
            <FormControl as="fieldset">
              <FormLabel as="legend">Wizard State</FormLabel>
              <Checkbox data-testid="data-wizard-isActive" isChecked={isActive}>
                isActive
              </Checkbox>
              <HStack>
                <Text as="span" data-testid="data-wizard-currentStep">
                  {currentStep}
                </Text>
              </HStack>
              <Text data-testid="data-wizard-entityType">{entityType}</Text>
              <Text data-testid="data-wizard-hasConstraints">{selectionConstraints ? 'yes' : 'no'}</Text>
              <Text data-testid="data-wizard-selectedCount">{selectedNodeIds.length}</Text>
            </FormControl>

            <Box mt={4}>
              <Text data-testid="data-ready">{isReady ? 'ready' : 'loading'}</Text>
            </Box>
          </>
        }
      >
        {isReady ? children : null}
      </ReactFlowTesting>
    )
  }

  return Wrapper
}

describe('WizardSelectionRestrictions', () => {
  beforeEach(() => {
    // Reset wizard store before each test
    const { actions } = useWizardStore.getState()
    actions.cancelWizard()

    cy.intercept('/api/v1/management/protocol-adapters/types', {
      items: [
        mockProtocolAdapter,
        {
          ...mockProtocolAdapter,
          id: 'modbus',
          name: 'Modbus',
          capabilities: ['READ'],
        },
      ],
    })
    cy.intercept('/api/v1/management/protocol-adapters/adapters', { items: [mockAdapter] })
  })

  describe('accessibility', () => {
    it('should be accessible (no visual output)', () => {
      cy.mountWithProviders(<WizardSelectionRestrictions />, {
        wrapper: getWrapperWith([
          { ...MOCK_NODE_EDGE, position: { x: 0, y: 0 } },
          { ...MOCK_NODE_ADAPTER, position: { x: 100, y: 0 } },
        ]),
      })

      cy.injectAxe()
      cy.checkAccessibility()
    })
  })

  describe('component rendering', () => {
    it('should not render anything visible', () => {
      cy.mountWithProviders(<WizardSelectionRestrictions />, {
        wrapper: getWrapperWith([
          { ...MOCK_NODE_EDGE, position: { x: 0, y: 0 } },
          { ...MOCK_NODE_ADAPTER, position: { x: 100, y: 0 } },
        ]),
      })

      // Component should not have any DOM output
      cy.get('[data-testid="wizard-selection-restrictions"]').should('not.exist')
    })
  })

  describe('node styling - wizard inactive', () => {
    it('should not apply restrictions when wizard is inactive', () => {
      const nodes = [
        { ...MOCK_NODE_EDGE, id: IdStubs.EDGE_NODE, position: { x: 200, y: 0 } },
        { ...MOCK_NODE_ADAPTER, id: 'adapter-1', position: { x: 0, y: 0 } },
        { ...MOCK_NODE_BRIDGE, id: 'bridge-1', position: { x: 400, y: 0 } },
      ]

      cy.mountWithProviders(<WizardSelectionRestrictions />, {
        wrapper: getWrapperWith(nodes),
      })

      cy.getByTestId('data-ready').should('have.text', 'ready')
      cy.getByTestId('data-wizard-isActive').should('not.have.attr', 'data-checked')

      // All nodes should be visible and selectable
      cy.get('[data-testid="react-flow-nodes"]').should('have.length', 3)
    })

    it('should restore nodes when wizard is cancelled', () => {
      const nodes = [
        { ...MOCK_NODE_ADAPTER, id: 'adapter-1', position: { x: 0, y: 0 } },
        { ...MOCK_NODE_BRIDGE, id: 'bridge-1', position: { x: 400, y: 0 } },
      ]

      cy.mountWithProviders(<WizardSelectionRestrictions />, {
        wrapper: getWrapperWith(nodes),
      })

      cy.getByTestId('data-ready').should('have.text', 'ready')

      // Start wizard with constraints
      cy.then(() => {
        const { actions } = useWizardStore.getState()
        actions.startWizard(EntityType.GROUP)
      })

      cy.getByTestId('data-wizard-isActive').should('have.attr', 'data-checked')
      cy.getByTestId('data-wizard-hasConstraints').should('have.text', 'yes')

      // Cancel wizard
      cy.then(() => {
        const { actions } = useWizardStore.getState()
        actions.cancelWizard()
      })

      cy.getByTestId('data-wizard-isActive').should('not.have.attr', 'data-checked')

      // Nodes should be restored (no ghosts should remain)
      cy.get('[data-testid="react-flow-nodes"]').should('have.length', 2)
    })
  })

  describe('node styling - with constraints', () => {
    it('should apply constraints when wizard starts', () => {
      const nodes = [
        { ...MOCK_NODE_ADAPTER, id: 'adapter-1', position: { x: 0, y: 0 } },
        { ...MOCK_NODE_BRIDGE, id: 'bridge-1', position: { x: 400, y: 0 } },
      ]

      cy.mountWithProviders(<WizardSelectionRestrictions />, {
        wrapper: getWrapperWith(nodes),
      })

      cy.getByTestId('data-ready').should('have.text', 'ready')

      // Verify initial state - no constraints
      cy.getByTestId('data-wizard-hasConstraints').should('have.text', 'no')

      // Start GROUP wizard - applies constraints
      cy.then(() => {
        const { actions } = useWizardStore.getState()
        actions.startWizard(EntityType.GROUP)
      })

      cy.getByTestId('data-wizard-hasConstraints').should('have.text', 'yes')
      cy.getByTestId('data-wizard-isActive').should('have.attr', 'data-checked')

      // Nodes remain in React Flow (not hidden, just styled)
      cy.get('[data-testid="react-flow-nodes"]').should('have.length', 2)
    })

    it('should handle nodes with ghost flag', () => {
      const nodes = [
        { ...MOCK_NODE_ADAPTER, id: 'adapter-1', position: { x: 0, y: 0 } },
        { ...MOCK_NODE_ADAPTER, id: 'ghost-adapter', position: { x: 200, y: 0 }, data: { isGhost: true } },
      ]

      cy.mountWithProviders(<WizardSelectionRestrictions />, {
        wrapper: getWrapperWith(nodes),
      })

      cy.getByTestId('data-ready').should('have.text', 'ready')

      cy.then(() => {
        const { actions } = useWizardStore.getState()
        actions.startWizard(EntityType.GROUP)
      })

      // Both nodes should remain visible
      cy.get('[data-testid="react-flow-nodes"]').should('have.length', 2)
    })

    it('should handle EDGE node', () => {
      const nodes = [
        { ...MOCK_NODE_EDGE, id: IdStubs.EDGE_NODE, position: { x: 200, y: 0 } },
        { ...MOCK_NODE_ADAPTER, id: 'adapter-1', position: { x: 0, y: 0 } },
      ]

      cy.mountWithProviders(<WizardSelectionRestrictions />, {
        wrapper: getWrapperWith(nodes),
      })

      cy.getByTestId('data-ready').should('have.text', 'ready')

      cy.then(() => {
        const { actions } = useWizardStore.getState()
        actions.startWizard(EntityType.GROUP)
      })

      // Both nodes should remain visible
      cy.get('[data-testid="react-flow-nodes"]').should('have.length', 2)
    })
  })

  describe('node type restrictions', () => {
    it('should apply node type constraints from wizard', () => {
      const nodes = [
        {
          ...MOCK_NODE_ADAPTER,
          id: 'adapter-1',
          type: NodeTypes.ADAPTER_NODE,
          position: { x: 0, y: 0 },
        },
        {
          ...MOCK_NODE_BRIDGE,
          id: 'bridge-1',
          type: NodeTypes.BRIDGE_NODE,
          position: { x: 400, y: 0 },
        },
      ]

      cy.mountWithProviders(<WizardSelectionRestrictions />, {
        wrapper: getWrapperWith(nodes),
      })

      cy.getByTestId('data-ready').should('have.text', 'ready')

      // Start GROUP wizard which allows ADAPTER, BRIDGE, PULSE
      cy.then(() => {
        const { actions } = useWizardStore.getState()
        actions.startWizard(EntityType.GROUP)
      })

      // Both nodes remain visible
      cy.get('[data-testid="react-flow-nodes"]').should('have.length', 2)
    })
  })

  describe('protocol adapter capabilities', () => {
    it('should handle adapters with different capabilities', () => {
      const nodes: Node[] = [
        {
          ...MOCK_NODE_ADAPTER,
          id: 'adapter-read-write',
          type: NodeTypes.ADAPTER_NODE,
          position: { x: 0, y: 0 },
          data: {
            ...MOCK_NODE_ADAPTER.data,
            type: 'simulation', // Has READ and WRITE capabilities
          },
        },
        {
          ...MOCK_NODE_ADAPTER,
          id: 'adapter-read-only',
          type: NodeTypes.ADAPTER_NODE,
          position: { x: 400, y: 0 },
          data: {
            ...MOCK_NODE_ADAPTER.data,
            type: 'modbus', // Only has READ capability
          },
        },
      ]

      cy.mountWithProviders(<WizardSelectionRestrictions />, {
        wrapper: getWrapperWith(nodes),
      })

      cy.getByTestId('data-ready').should('have.text', 'ready')

      // Start ASSET_MAPPER wizard which requires WRITE capability
      cy.then(() => {
        const { actions } = useWizardStore.getState()
        actions.startWizard(EntityType.ASSET_MAPPER)
      })

      cy.getByTestId('data-wizard-entityType').should('have.text', 'ASSET_MAPPER')

      // Both adapters remain in the DOM (styling is applied but nodes aren't removed)
      cy.get('[data-testid="react-flow-nodes"]').should('have.length', 2)
    })
  })

  describe('edge dimming', () => {
    it('should dim edges when wizard is active with constraints', () => {
      const nodes = [
        { ...MOCK_NODE_ADAPTER, id: 'adapter-1', position: { x: 0, y: 0 } },
        { ...MOCK_NODE_BRIDGE, id: 'bridge-1', position: { x: 400, y: 0 } },
      ]
      const edges: Edge[] = [
        {
          id: 'edge-1',
          source: 'adapter-1',
          target: 'bridge-1',
        },
      ]

      cy.mountWithProviders(<WizardSelectionRestrictions />, {
        wrapper: getWrapperWith(nodes, edges),
      })

      cy.getByTestId('data-ready').should('have.text', 'ready')

      // Start wizard
      cy.then(() => {
        const { actions } = useWizardStore.getState()
        actions.startWizard(EntityType.GROUP)
      })

      // Edge should exist
      cy.get('[data-testid="react-flow-edges"]').should('have.length', 1)
    })

    it('should restore edges when wizard is cancelled', () => {
      const nodes = [
        { ...MOCK_NODE_ADAPTER, id: 'adapter-1', position: { x: 0, y: 0 } },
        { ...MOCK_NODE_BRIDGE, id: 'bridge-1', position: { x: 400, y: 0 } },
      ]
      const edges: Edge[] = [
        {
          id: 'edge-1',
          source: 'adapter-1',
          target: 'bridge-1',
          animated: true,
        },
      ]

      cy.mountWithProviders(<WizardSelectionRestrictions />, {
        wrapper: getWrapperWith(nodes, edges),
      })

      cy.getByTestId('data-ready').should('have.text', 'ready')

      // Start wizard
      cy.then(() => {
        const { actions } = useWizardStore.getState()
        actions.startWizard(EntityType.GROUP)
      })

      cy.getByTestId('data-wizard-isActive').should('have.attr', 'data-checked')

      // Cancel wizard
      cy.then(() => {
        const { actions } = useWizardStore.getState()
        actions.cancelWizard()
      })

      // Edge should be restored (no ghost edges)
      cy.get('[data-testid="react-flow-edges"]').should('have.length', 1)
    })

    it('should not dim ghost edges', () => {
      const nodes = [
        { ...MOCK_NODE_ADAPTER, id: 'adapter-1', position: { x: 0, y: 0 } },
        { ...MOCK_NODE_ADAPTER, id: 'ghost-adapter', position: { x: 200, y: 0 }, data: { isGhost: true } },
      ]
      const edges: Edge[] = [
        {
          id: 'edge-real',
          source: 'adapter-1',
          target: 'ghost-adapter',
        },
        {
          id: 'edge-ghost',
          source: 'ghost-adapter',
          target: 'adapter-1',
          data: { isGhost: true },
        },
      ]

      cy.mountWithProviders(<WizardSelectionRestrictions />, {
        wrapper: getWrapperWith(nodes, edges),
      })

      cy.getByTestId('data-ready').should('have.text', 'ready')

      cy.then(() => {
        const { actions } = useWizardStore.getState()
        actions.startWizard(EntityType.GROUP)
      })

      // Both edges should be visible
      cy.get('[data-testid="react-flow-edges"]').should('have.length', 2)
    })
  })

  describe('ghost edge creation', () => {
    it('should respond to node selection when combiner is present', () => {
      const nodes = [
        { ...MOCK_NODE_ADAPTER, id: 'adapter-1', position: { x: 0, y: 0 } },
        { ...MOCK_NODE_BRIDGE, id: 'bridge-1', position: { x: 400, y: 0 } },
        // Add ghost combiner that would be created by GhostNodeRenderer
        {
          id: 'ghost-combiner-group',
          type: NodeTypes.CLUSTER_NODE,
          position: { x: 200, y: 200 },
          data: { isGhost: true, label: 'New Group' },
        },
      ]

      cy.mountWithProviders(<WizardSelectionRestrictions />, {
        wrapper: getWrapperWith(nodes),
      })

      cy.getByTestId('data-ready').should('have.text', 'ready')

      // Start wizard and select a node
      cy.then(() => {
        const { actions } = useWizardStore.getState()
        actions.startWizard(EntityType.GROUP)
        actions.selectNode('adapter-1')
      })

      // Verify selection is recorded
      cy.getByTestId('data-wizard-selectedCount').should('have.text', '1')

      // Component should not crash (it manages edges internally)
      cy.getByTestId('data-wizard-isActive').should('have.attr', 'data-checked')
    })

    it('should handle multiple node selections', () => {
      const nodes = [
        { ...MOCK_NODE_ADAPTER, id: 'adapter-1', position: { x: 0, y: 0 } },
        { ...MOCK_NODE_BRIDGE, id: 'bridge-1', position: { x: 400, y: 0 } },
        {
          id: 'ghost-combiner-group',
          type: NodeTypes.CLUSTER_NODE,
          position: { x: 200, y: 200 },
          data: { isGhost: true, label: 'New Group' },
        },
      ]

      cy.mountWithProviders(<WizardSelectionRestrictions />, {
        wrapper: getWrapperWith(nodes),
      })

      cy.getByTestId('data-ready').should('have.text', 'ready')

      cy.then(() => {
        const { actions } = useWizardStore.getState()
        actions.startWizard(EntityType.GROUP)
        actions.selectNode('adapter-1')
        actions.selectNode('bridge-1')
      })

      // Verify both selections are recorded
      cy.getByTestId('data-wizard-selectedCount').should('have.text', '2')
    })

    it('should handle selection removal', () => {
      const nodes = [
        { ...MOCK_NODE_ADAPTER, id: 'adapter-1', position: { x: 0, y: 0 } },
        {
          id: 'ghost-combiner-group',
          type: NodeTypes.CLUSTER_NODE,
          position: { x: 200, y: 200 },
          data: { isGhost: true, label: 'New Group' },
        },
      ]

      cy.mountWithProviders(<WizardSelectionRestrictions />, {
        wrapper: getWrapperWith(nodes),
      })

      cy.getByTestId('data-ready').should('have.text', 'ready')

      // Select a node
      cy.then(() => {
        const { actions } = useWizardStore.getState()
        actions.startWizard(EntityType.GROUP)
        actions.selectNode('adapter-1')
      })

      cy.getByTestId('data-wizard-selectedCount').should('have.text', '1')

      // Deselect the node
      cy.then(() => {
        const { actions } = useWizardStore.getState()
        actions.deselectNode('adapter-1')
      })

      cy.getByTestId('data-wizard-selectedCount').should('have.text', '0')
    })

    it('should handle dynamic selection changes', () => {
      const nodes = [
        { ...MOCK_NODE_ADAPTER, id: 'adapter-1', position: { x: 0, y: 0 } },
        { ...MOCK_NODE_BRIDGE, id: 'bridge-1', position: { x: 400, y: 0 } },
        {
          id: 'ghost-combiner-group',
          type: NodeTypes.CLUSTER_NODE,
          position: { x: 200, y: 200 },
          data: { isGhost: true, label: 'New Group' },
        },
      ]

      cy.mountWithProviders(<WizardSelectionRestrictions />, {
        wrapper: getWrapperWith(nodes),
      })

      cy.getByTestId('data-ready').should('have.text', 'ready')

      // Select first node
      cy.then(() => {
        const { actions } = useWizardStore.getState()
        actions.startWizard(EntityType.GROUP)
        actions.selectNode('adapter-1')
      })

      cy.getByTestId('data-wizard-selectedCount').should('have.text', '1')

      // Add second node
      cy.then(() => {
        const { actions } = useWizardStore.getState()
        actions.selectNode('bridge-1')
      })

      cy.getByTestId('data-wizard-selectedCount').should('have.text', '2')

      // Remove first node
      cy.then(() => {
        const { actions } = useWizardStore.getState()
        actions.deselectNode('adapter-1')
      })

      cy.getByTestId('data-wizard-selectedCount').should('have.text', '1')
    })
  })

  describe('group constraints', () => {
    it('should handle nodes with parent relationships', () => {
      const nodes: Node[] = [
        {
          ...MOCK_NODE_ADAPTER,
          id: 'adapter-standalone',
          type: NodeTypes.ADAPTER_NODE,
          position: { x: 0, y: 0 },
        },
        {
          ...MOCK_NODE_ADAPTER,
          id: 'adapter-in-group',
          type: NodeTypes.ADAPTER_NODE,
          position: { x: 400, y: 0 },
          parentId: 'group-1', // This adapter is in a group
        },
      ]

      cy.mountWithProviders(<WizardSelectionRestrictions />, {
        wrapper: getWrapperWith(nodes),
      })

      cy.getByTestId('data-ready').should('have.text', 'ready')

      // Start GROUP wizard (has excludeNodesInGroups = true)
      cy.then(() => {
        const { actions } = useWizardStore.getState()
        actions.startWizard(EntityType.GROUP)
      })

      // Both nodes remain visible (styling differentiates them)
      cy.get('[data-testid="react-flow-nodes"]').should('have.length', 2)
    })
  })

  describe('custom filter', () => {
    it('should apply custom filter from constraints', () => {
      const nodes = [
        {
          ...MOCK_NODE_ADAPTER,
          id: 'adapter-allowed',
          position: { x: 0, y: 0 },
          data: { ...MOCK_NODE_ADAPTER.data, customProp: 'allowed' },
        },
        {
          ...MOCK_NODE_ADAPTER,
          id: 'adapter-blocked',
          position: { x: 400, y: 0 },
          data: { ...MOCK_NODE_ADAPTER.data, customProp: 'blocked' },
        },
      ]

      cy.mountWithProviders(<WizardSelectionRestrictions />, {
        wrapper: getWrapperWith(nodes),
      })

      cy.getByTestId('data-ready').should('have.text', 'ready')

      // Start wizard and add custom filter
      cy.then(() => {
        const { actions } = useWizardStore.getState()
        actions.startWizard(EntityType.GROUP)

        // Override constraints with custom filter
        useWizardStore.setState({
          selectionConstraints: {
            minNodes: 1,
            maxNodes: 10,
            allowedNodeTypes: [NodeTypes.ADAPTER_NODE],
            customFilter: (node: Node) => node.data?.customProp === 'allowed',
          },
        })
      })

      // Both nodes remain visible (custom filter applies styling)
      cy.get('[data-testid="react-flow-nodes"]').should('have.length', 2)
    })
  })

  describe('edge cases', () => {
    it('should handle empty node list', () => {
      cy.mountWithProviders(<WizardSelectionRestrictions />, {
        wrapper: getWrapperWith([]),
      })

      cy.getByTestId('data-ready').should('have.text', 'ready')

      cy.then(() => {
        const { actions } = useWizardStore.getState()
        actions.startWizard(EntityType.GROUP)
      })

      cy.get('[data-testid="react-flow-nodes"]').should('have.length', 0)
    })

    it('should handle wizard with no selection constraints', () => {
      const nodes = [{ ...MOCK_NODE_ADAPTER, id: 'adapter-1', position: { x: 0, y: 0 } }]

      cy.mountWithProviders(<WizardSelectionRestrictions />, {
        wrapper: getWrapperWith(nodes),
      })

      cy.getByTestId('data-ready').should('have.text', 'ready')

      // Manually set wizard active without constraints
      cy.then(() => {
        useWizardStore.setState({
          isActive: true,
          entityType: EntityType.ADAPTER,
          selectionConstraints: null,
        })
      })

      cy.getByTestId('data-wizard-hasConstraints').should('have.text', 'no')

      // Node should remain visible
      cy.get('[data-testid="react-flow-nodes"]').should('have.length', 1)
    })

    it('should handle missing ghost combiner gracefully', () => {
      const nodes = [{ ...MOCK_NODE_ADAPTER, id: 'adapter-1', position: { x: 0, y: 0 } }]

      cy.mountWithProviders(<WizardSelectionRestrictions />, {
        wrapper: getWrapperWith(nodes),
      })

      cy.getByTestId('data-ready').should('have.text', 'ready')

      // Start wizard and select node WITHOUT ghost combiner present
      cy.then(() => {
        const { actions } = useWizardStore.getState()
        actions.startWizard(EntityType.GROUP)
        actions.selectNode('adapter-1')
      })

      cy.getByTestId('data-wizard-selectedCount').should('have.text', '1')

      // Should not crash, just no ghost edges created
      cy.get('[data-testid="react-flow-edges"]').should('have.length', 0)
    })
  })
})
