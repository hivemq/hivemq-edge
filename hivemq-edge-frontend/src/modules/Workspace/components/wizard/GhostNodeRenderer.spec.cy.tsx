import React from 'react'
import { Checkbox, FormControl, FormLabel, HStack, Text } from '@chakra-ui/react'
import { type Node } from '@xyflow/react'
import { MOCK_NODE_ADAPTER, MOCK_NODE_BRIDGE, MOCK_NODE_EDGE } from '@/__test-utils__/react-flow/nodes.ts'
import { ReactFlowTesting } from '@/__test-utils__/react-flow/ReactFlowTesting.tsx'
import { mockBridge } from '@/api/hooks/useGetBridges/__handlers__'
import { mockAdapter, mockProtocolAdapter } from '@/api/hooks/useProtocolAdapters/__handlers__'
import { IdStubs } from '@/modules/Workspace/types'
import { useWizardStore } from '@/modules/Workspace/hooks/useWizardStore'
import GhostNodeRenderer from './GhostNodeRenderer'
import { EntityType, IntegrationPointType } from './types'

const getWrapperWith = (initialNodes?: Node[]) => {
  const Wrapper: React.JSXElementConstructor<{ children: React.ReactNode }> = ({ children }) => {
    const { isActive, entityType, currentStep, totalSteps } = useWizardStore()
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
          },
        }}
        showDashboard={true}
        showReactFlowElements={true}
        dashboard={
          <>
            <FormControl as="fieldset">
              <FormLabel as="legend">Current Task</FormLabel>
              <Checkbox data-testid="data-wizard-isActive" value="Sasuke" isChecked={isActive}>
                isActive
              </Checkbox>
              <HStack>
                <Text as="span" data-testid="data-wizard-currentStep">
                  {currentStep}
                </Text>
                <Text as="span">{' / '}</Text>
                <Text as="span" data-testid="data-wizard-totalSteps">
                  {totalSteps}
                </Text>
              </HStack>
              <Text data-testid="data-wizard-entityType">{entityType}</Text>
              <Text data-testid="data-wizard-ghostCount">
                Ghost Nodes: {useWizardStore.getState().ghostNodes.length}
              </Text>
            </FormControl>
            <Text data-testid="data-ready">{isReady ? 'ready' : 'loading'}</Text>
          </>
        }
      >
        {isReady ? children : null}
      </ReactFlowTesting>
    )
  }

  return Wrapper
}

describe('GhostNodeRenderer', () => {
  beforeEach(() => {
    // Reset wizard store before each test
    const { actions } = useWizardStore.getState()
    actions.cancelWizard()

    cy.intercept('/api/v1/management/protocol-adapters/types', { items: [mockProtocolAdapter] })
    cy.intercept('/api/v1/management/protocol-adapters/adapters', { items: [mockAdapter] })

    cy.intercept('/api/v1/management/bridges', {
      items: [mockBridge],
    })
  })

  it('should not render anything visible', () => {
    cy.mountWithProviders(<GhostNodeRenderer />, {
      wrapper: getWrapperWith([
        { ...MOCK_NODE_EDGE, id: IdStubs.EDGE_NODE, position: { x: 200, y: 0 } },
        { ...MOCK_NODE_ADAPTER, position: { x: 0, y: 0 } },
        { ...MOCK_NODE_BRIDGE, position: { x: 400, y: 0 } },
      ]),
    })

    // Component should not have any DOM output
    // It only manages React Flow state
    cy.get('[data-testid="ghost-node-renderer"]').should('not.exist')
    cy.getByTestId('data-wizard-isActive').should('not.have.attr', 'data-checked')
    cy.getByTestId('data-wizard-entityType').should('have.text', '')
    cy.getByTestId('data-wizard-currentStep').should('have.text', '0')
    cy.getByTestId('data-wizard-totalSteps').should('have.text', '0')
  })

  it('should add ghost node when wizard starts with entity that requires ghost', () => {
    const { actions } = useWizardStore.getState()

    cy.mountWithProviders(<GhostNodeRenderer />, {
      wrapper: getWrapperWith([
        { ...MOCK_NODE_EDGE, id: IdStubs.EDGE_NODE, position: { x: 200, y: 0 } },
        { ...MOCK_NODE_ADAPTER, position: { x: 0, y: 0 } },
        { ...MOCK_NODE_BRIDGE, position: { x: 400, y: 0 } },
      ]),
    })

    // Wait for wrapper to be ready
    cy.getByTestId('data-ready').should('have.text', 'ready')

    // Verify initial nodes are in React Flow
    cy.get('[data-testid="react-flow-nodes"]').should('have.length', 3)

    // Start wizard with ADAPTER (requires ghost)
    cy.wrap(null).then(() => {
      actions.startWizard(EntityType.ADAPTER)
    })

    cy.getByTestId('data-wizard-isActive').should('have.attr', 'data-checked')
    cy.getByTestId('data-wizard-entityType').should('have.text', 'ADAPTER')

    // Check wizard store has ghost nodes (ADAPTER creates 2 nodes: adapter + device)
    cy.wrap(null).then(() => {
      const { ghostNodes } = useWizardStore.getState()
      expect(ghostNodes.length).to.be.greaterThan(0)
      expect(ghostNodes[0].data.isGhost).to.be.true
    })

    // Check React Flow has the ghost nodes rendered
    cy.get('[data-testid="react-flow-nodes"]').should('have.length.greaterThan', 3)
  })

  it('should not add ghost node for entity that does not require ghost', () => {
    const { actions } = useWizardStore.getState()

    cy.mountWithProviders(<GhostNodeRenderer />, {
      wrapper: getWrapperWith([
        { ...MOCK_NODE_EDGE, id: IdStubs.EDGE_NODE, position: { x: 200, y: 0 } },
        { ...MOCK_NODE_ADAPTER, position: { x: 0, y: 0 } },
        { ...MOCK_NODE_BRIDGE, position: { x: 400, y: 0 } },
      ]),
    })

    // Start wizard with TAG (integration point - does not require ghost)
    cy.wrap(null).then(() => {
      actions.startWizard(IntegrationPointType.TAG)
    })

    cy.getByTestId('data-wizard-isActive').should('have.attr', 'data-checked')
    cy.getByTestId('data-wizard-entityType').should('have.text', 'TAG')

    // Check wizard store has NO ghost nodes for integration points
    cy.wrap(null).then(() => {
      const { ghostNodes } = useWizardStore.getState()
      expect(ghostNodes).to.have.length(0)
    })

    // Check React Flow has no ghost nodes rendered
    cy.get('[data-testid="react-flow-nodes"]').should('have.length', 3) // Only the initial nodes (EDGE, ADAPTER, BRIDGE)
  })

  it('should remove ghost nodes when wizard is cancelled', () => {
    const { actions } = useWizardStore.getState()

    cy.mountWithProviders(<GhostNodeRenderer />, {
      wrapper: getWrapperWith([
        { ...MOCK_NODE_EDGE, id: IdStubs.EDGE_NODE, position: { x: 200, y: 0 } },
        { ...MOCK_NODE_ADAPTER, position: { x: 0, y: 0 } },
        { ...MOCK_NODE_BRIDGE, position: { x: 400, y: 0 } },
      ]),
    })

    cy.getByTestId('data-ready').should('have.text', 'ready')

    // Start wizard
    cy.wrap(null).then(() => {
      actions.startWizard(EntityType.ADAPTER)
    })

    cy.getByTestId('data-wizard-isActive').should('have.attr', 'data-checked')

    // Verify ghost node exists in store
    cy.wrap(null).then(() => {
      const { ghostNodes } = useWizardStore.getState()
      expect(ghostNodes.length).to.be.greaterThan(0)
    })

    // Verify ghost nodes rendered in React Flow (should have more nodes than initial 3)
    cy.get('[data-testid="react-flow-nodes"]').should('have.length.greaterThan', 3)

    // Cancel wizard
    cy.wrap(null).then(() => {
      actions.cancelWizard()
    })

    cy.getByTestId('data-wizard-isActive').should('not.have.attr', 'data-checked')

    // Verify ghost nodes cleared from store
    cy.wrap(null).then(() => {
      const { ghostNodes } = useWizardStore.getState()
      expect(ghostNodes).to.have.length(0)
    })

    // Verify ghost nodes removed from React Flow
    cy.get('[data-testid="react-flow-nodes"]').should('have.length', 3) // Back to initial nodes
  })

  it('should keep ghost nodes visible throughout the wizard', () => {
    const { actions } = useWizardStore.getState()

    cy.mountWithProviders(<GhostNodeRenderer />, {
      wrapper: getWrapperWith([
        { ...MOCK_NODE_EDGE, id: IdStubs.EDGE_NODE, position: { x: 200, y: 0 } },
        { ...MOCK_NODE_ADAPTER, position: { x: 0, y: 0 } },
        { ...MOCK_NODE_BRIDGE, position: { x: 400, y: 0 } },
      ]),
    })

    cy.getByTestId('data-ready').should('have.text', 'ready')

    // Start wizard
    cy.wrap(null).then(() => {
      actions.startWizard(EntityType.ADAPTER)
    })

    cy.getByTestId('data-wizard-currentStep').should('have.text', '0')

    // Step 0 should have ghost nodes
    cy.wrap(null).then(() => {
      const { ghostNodes, currentStep } = useWizardStore.getState()
      expect(currentStep).to.equal(0)
      expect(ghostNodes.length).to.be.greaterThan(0)
    })

    cy.get('[data-testid="react-flow-nodes"]').should('have.length.greaterThan', 3)

    // Move to step 1
    cy.wrap(null).then(() => {
      actions.nextStep()
    })

    cy.getByTestId('data-wizard-currentStep').should('have.text', '1')

    // Step 1 should STILL have ghost nodes (they persist throughout the wizard)
    cy.wrap(null).then(() => {
      const { ghostNodes, currentStep } = useWizardStore.getState()
      expect(currentStep).to.equal(1)
      expect(ghostNodes.length).to.be.greaterThan(0)
    })

    // Ghost nodes should still be visible in React Flow
    cy.get('[data-testid="react-flow-nodes"]').should('have.length.greaterThan', 3)
  })

  it('should create appropriate ghost node type for ADAPTER entity', () => {
    const { actions } = useWizardStore.getState()

    cy.mountWithProviders(<GhostNodeRenderer />, {
      wrapper: getWrapperWith([
        { ...MOCK_NODE_EDGE, id: IdStubs.EDGE_NODE, position: { x: 200, y: 0 } },
        { ...MOCK_NODE_ADAPTER, position: { x: 0, y: 0 } },
        { ...MOCK_NODE_BRIDGE, position: { x: 400, y: 0 } },
      ]),
    })

    cy.getByTestId('data-ready').should('have.text', 'ready')

    cy.wrap(null).then(() => {
      actions.startWizard(EntityType.ADAPTER)
    })

    cy.getByTestId('data-wizard-entityType').should('have.text', 'ADAPTER')

    cy.wrap(null).then(() => {
      const { ghostNodes } = useWizardStore.getState()
      expect(ghostNodes).to.have.length.greaterThan(0)
      const hasAdapterNode = ghostNodes.some((node) => node.type === 'ADAPTER_NODE')
      expect(hasAdapterNode).to.be.true
    })
  })

  it('should create appropriate ghost node type for BRIDGE entity', () => {
    const { actions } = useWizardStore.getState()

    cy.mountWithProviders(<GhostNodeRenderer />, {
      wrapper: getWrapperWith([
        { ...MOCK_NODE_EDGE, id: IdStubs.EDGE_NODE, position: { x: 200, y: 0 } },
        { ...MOCK_NODE_ADAPTER, position: { x: 0, y: 0 } },
        { ...MOCK_NODE_BRIDGE, position: { x: 400, y: 0 } },
      ]),
    })

    cy.getByTestId('data-ready').should('have.text', 'ready')

    cy.wrap(null).then(() => {
      actions.startWizard(EntityType.BRIDGE)
    })

    cy.getByTestId('data-wizard-entityType').should('have.text', 'BRIDGE')

    cy.wrap(null).then(() => {
      const { ghostNodes } = useWizardStore.getState()
      expect(ghostNodes).to.have.length.greaterThan(0)
      const hasBridgeNode = ghostNodes.some((node) => node.type === 'BRIDGE_NODE')
      expect(hasBridgeNode).to.be.true
    })
  })

  it('should create appropriate ghost node type for COMBINER entity', () => {
    const { actions } = useWizardStore.getState()

    cy.mountWithProviders(<GhostNodeRenderer />, {
      wrapper: getWrapperWith([
        { ...MOCK_NODE_EDGE, id: IdStubs.EDGE_NODE, position: { x: 200, y: 0 } },
        { ...MOCK_NODE_ADAPTER, position: { x: 0, y: 0 } },
        { ...MOCK_NODE_BRIDGE, position: { x: 400, y: 0 } },
      ]),
    })

    cy.getByTestId('data-ready').should('have.text', 'ready')

    cy.wrap(null).then(() => {
      actions.startWizard(EntityType.COMBINER)
    })

    cy.getByTestId('data-wizard-entityType').should('have.text', 'COMBINER')

    cy.wrap(null).then(() => {
      const { ghostNodes } = useWizardStore.getState()
      expect(ghostNodes).to.have.length.greaterThan(0)
      const hasCombinerNode = ghostNodes.some((node) => node.type === 'COMBINER_NODE')
      expect(hasCombinerNode).to.be.true
    })
  })

  it('should cleanup ghost nodes via cancelWizard (unmount cleanup tested via cancel)', () => {
    const { actions } = useWizardStore.getState()

    cy.mountWithProviders(<GhostNodeRenderer />, {
      wrapper: getWrapperWith([
        { ...MOCK_NODE_EDGE, id: IdStubs.EDGE_NODE, position: { x: 200, y: 0 } },
        { ...MOCK_NODE_ADAPTER, position: { x: 0, y: 0 } },
        { ...MOCK_NODE_BRIDGE, position: { x: 400, y: 0 } },
      ]),
    })

    cy.getByTestId('data-ready').should('have.text', 'ready')

    cy.wrap(null).then(() => {
      actions.startWizard(EntityType.ADAPTER)
    })

    cy.wrap(null).then(() => {
      const { ghostNodes } = useWizardStore.getState()
      expect(ghostNodes.length).to.be.greaterThan(0)
    })

    cy.get('[data-testid="react-flow-nodes"]').should('have.length.greaterThan', 3)

    cy.wrap(null).then(() => {
      actions.cancelWizard()
    })

    cy.wrap(null).then(() => {
      const { ghostNodes } = useWizardStore.getState()
      expect(ghostNodes).to.have.length(0)
    })

    cy.get('[data-testid="react-flow-nodes"]').should('have.length', 3)
  })

  it('should not create duplicate ghost nodes', () => {
    const { actions } = useWizardStore.getState()

    cy.mountWithProviders(<GhostNodeRenderer />, {
      wrapper: getWrapperWith([
        { ...MOCK_NODE_EDGE, id: IdStubs.EDGE_NODE, position: { x: 200, y: 0 } },
        { ...MOCK_NODE_ADAPTER, position: { x: 0, y: 0 } },
        { ...MOCK_NODE_BRIDGE, position: { x: 400, y: 0 } },
      ]),
    })

    cy.getByTestId('data-ready').should('have.text', 'ready')

    cy.wrap(null).then(() => {
      actions.startWizard(EntityType.ADAPTER)
    })

    cy.getByTestId('data-wizard-isActive').should('have.attr', 'data-checked')

    let initialGhostCount: number
    cy.wrap(null).then(() => {
      const { ghostNodes } = useWizardStore.getState()
      initialGhostCount = ghostNodes.length
      expect(initialGhostCount).to.be.greaterThan(0)
    })

    cy.wrap(null).then(() => {
      const { ghostNodes } = useWizardStore.getState()
      expect(ghostNodes).to.have.length(initialGhostCount)
    })

    cy.get('[data-testid="react-flow-nodes"]').should('have.length.greaterThan', 3)
  })

  it('should handle rapid wizard start/cancel cycles', () => {
    const { actions } = useWizardStore.getState()

    cy.mountWithProviders(<GhostNodeRenderer />, {
      wrapper: getWrapperWith([
        { ...MOCK_NODE_EDGE, id: IdStubs.EDGE_NODE, position: { x: 200, y: 0 } },
        { ...MOCK_NODE_ADAPTER, position: { x: 0, y: 0 } },
        { ...MOCK_NODE_BRIDGE, position: { x: 400, y: 0 } },
      ]),
    })

    cy.getByTestId('data-ready').should('have.text', 'ready')

    cy.wrap(null).then(() => {
      actions.startWizard(EntityType.ADAPTER)
    })

    cy.getByTestId('data-wizard-entityType').should('have.text', 'ADAPTER')

    cy.wrap(null).then(() => {
      actions.cancelWizard()
    })

    cy.getByTestId('data-wizard-isActive').should('not.have.attr', 'data-checked')

    cy.wrap(null).then(() => {
      actions.startWizard(EntityType.BRIDGE)
    })

    cy.getByTestId('data-wizard-entityType').should('have.text', 'BRIDGE')

    cy.wrap(null).then(() => {
      actions.cancelWizard()
    })

    cy.getByTestId('data-wizard-isActive').should('not.have.attr', 'data-checked')

    cy.wrap(null).then(() => {
      actions.startWizard(EntityType.COMBINER)
    })

    cy.getByTestId('data-wizard-entityType').should('have.text', 'COMBINER')

    cy.wrap(null).then(() => {
      const { ghostNodes } = useWizardStore.getState()
      expect(ghostNodes.length).to.be.greaterThan(0)
      const hasCombinerNode = ghostNodes.some((node) => node.type === 'COMBINER_NODE')
      expect(hasCombinerNode).to.be.true
      const hasAdapterNode = ghostNodes.some((node) => node.type === 'ADAPTER_NODE')
      const hasBridgeNode = ghostNodes.some((node) => node.type === 'BRIDGE_NODE')
      expect(hasAdapterNode).to.be.false
      expect(hasBridgeNode).to.be.false
    })

    cy.get('[data-testid="react-flow-nodes"]').should('have.length.greaterThan', 3)
  })

  it('should create appropriate ghost node type for ASSET_MAPPER entity', () => {
    const { actions } = useWizardStore.getState()

    cy.mountWithProviders(<GhostNodeRenderer />, {
      wrapper: getWrapperWith([
        { ...MOCK_NODE_EDGE, id: IdStubs.EDGE_NODE, position: { x: 200, y: 0 } },
        { ...MOCK_NODE_ADAPTER, position: { x: 0, y: 0 } },
        { ...MOCK_NODE_BRIDGE, position: { x: 400, y: 0 } },
      ]),
    })

    cy.getByTestId('data-ready').should('have.text', 'ready')

    cy.wrap(null).then(() => {
      actions.startWizard(EntityType.ASSET_MAPPER)
    })

    cy.getByTestId('data-wizard-entityType').should('have.text', 'ASSET_MAPPER')

    cy.wrap(null).then(() => {
      const { ghostNodes } = useWizardStore.getState()
      expect(ghostNodes).to.have.length.greaterThan(0)
      const hasAssetMapperNode = ghostNodes.some(
        (node) => node.id.includes('ghost-assetmapper-') || node.type === 'COMBINER_NODE'
      )
      expect(hasAssetMapperNode).to.be.true
    })
  })

  it('should verify ghost nodes have isGhost flag in data', () => {
    const { actions } = useWizardStore.getState()

    cy.mountWithProviders(<GhostNodeRenderer />, {
      wrapper: getWrapperWith([
        { ...MOCK_NODE_EDGE, id: IdStubs.EDGE_NODE, position: { x: 200, y: 0 } },
        { ...MOCK_NODE_ADAPTER, position: { x: 0, y: 0 } },
        { ...MOCK_NODE_BRIDGE, position: { x: 400, y: 0 } },
      ]),
    })

    cy.getByTestId('data-ready').should('have.text', 'ready')

    cy.wrap(null).then(() => {
      actions.startWizard(EntityType.ADAPTER)
    })

    cy.wrap(null).then(() => {
      const { ghostNodes } = useWizardStore.getState()
      expect(ghostNodes.length).to.be.greaterThan(0)
      ghostNodes.forEach((node) => {
        expect(node.data.isGhost).to.be.true
      })
    })
  })

  it('should create ghost edges along with ghost nodes', () => {
    const { actions } = useWizardStore.getState()

    cy.mountWithProviders(<GhostNodeRenderer />, {
      wrapper: getWrapperWith([
        { ...MOCK_NODE_EDGE, id: IdStubs.EDGE_NODE, position: { x: 200, y: 0 } },
        { ...MOCK_NODE_ADAPTER, position: { x: 0, y: 0 } },
        { ...MOCK_NODE_BRIDGE, position: { x: 400, y: 0 } },
      ]),
    })

    cy.getByTestId('data-ready').should('have.text', 'ready')

    cy.wrap(null).then(() => {
      actions.startWizard(EntityType.ADAPTER)
    })

    cy.wrap(null).then(() => {
      const { ghostNodes, ghostEdges } = useWizardStore.getState()
      expect(ghostNodes.length).to.be.greaterThan(0)
      expect(ghostEdges.length).to.be.greaterThan(0)
      ghostEdges.forEach((edge) => {
        expect(edge.data?.isGhost).to.be.true
      })
    })

    cy.get('[data-testid="react-flow-nodes"]').should('have.length.greaterThan', 3)
    cy.get('[data-testid="react-flow-edges"]').should('exist')
  })

  it('should clear both ghost nodes and edges when wizard is cancelled', () => {
    const { actions } = useWizardStore.getState()

    cy.mountWithProviders(<GhostNodeRenderer />, {
      wrapper: getWrapperWith([
        { ...MOCK_NODE_EDGE, id: IdStubs.EDGE_NODE, position: { x: 200, y: 0 } },
        { ...MOCK_NODE_ADAPTER, position: { x: 0, y: 0 } },
        { ...MOCK_NODE_BRIDGE, position: { x: 400, y: 0 } },
      ]),
    })

    cy.getByTestId('data-ready').should('have.text', 'ready')

    cy.wrap(null).then(() => {
      actions.startWizard(EntityType.BRIDGE)
    })

    cy.wrap(null).then(() => {
      const { ghostNodes, ghostEdges } = useWizardStore.getState()
      expect(ghostNodes.length).to.be.greaterThan(0)
      expect(ghostEdges.length).to.be.greaterThan(0)
    })

    cy.wrap(null).then(() => {
      actions.cancelWizard()
    })

    cy.wrap(null).then(() => {
      const { ghostNodes, ghostEdges } = useWizardStore.getState()
      expect(ghostNodes).to.have.length(0)
      expect(ghostEdges).to.have.length(0)
    })

    cy.get('[data-testid="react-flow-nodes"]').should('have.length', 3)
  })

  it('should maintain ghost visibility after navigating steps', () => {
    const { actions } = useWizardStore.getState()

    cy.mountWithProviders(<GhostNodeRenderer />, {
      wrapper: getWrapperWith([
        { ...MOCK_NODE_EDGE, id: IdStubs.EDGE_NODE, position: { x: 200, y: 0 } },
        { ...MOCK_NODE_ADAPTER, position: { x: 0, y: 0 } },
        { ...MOCK_NODE_BRIDGE, position: { x: 400, y: 0 } },
      ]),
    })

    cy.getByTestId('data-ready').should('have.text', 'ready')

    cy.wrap(null).then(() => {
      actions.startWizard(EntityType.ADAPTER)
    })

    let initialGhostCount: number
    cy.wrap(null).then(() => {
      const { ghostNodes } = useWizardStore.getState()
      initialGhostCount = ghostNodes.length
      expect(initialGhostCount).to.be.greaterThan(0)
    })

    cy.wrap(null).then(() => {
      actions.nextStep()
    })

    cy.getByTestId('data-wizard-currentStep').should('have.text', '1')

    cy.wrap(null).then(() => {
      const { ghostNodes } = useWizardStore.getState()
      expect(ghostNodes).to.have.length(initialGhostCount)
    })

    cy.wrap(null).then(() => {
      actions.nextStep()
    })

    cy.getByTestId('data-wizard-currentStep').should('have.text', '2')

    cy.wrap(null).then(() => {
      const { ghostNodes } = useWizardStore.getState()
      expect(ghostNodes).to.have.length(initialGhostCount)
    })
  })
})
