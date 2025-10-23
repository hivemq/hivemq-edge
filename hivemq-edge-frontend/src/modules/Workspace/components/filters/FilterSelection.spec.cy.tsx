import { useMemo } from 'react'
import type { JSXElementConstructor, ChangeEvent } from 'react'
import type { Edge, Node, NodeSelectionChange } from '@xyflow/react'
import { Checkbox, CheckboxGroup, HStack, Text, VStack } from '@chakra-ui/react'

import { MOCK_NODE_ADAPTER, MOCK_NODE_COMBINER, MOCK_NODE_DEVICE } from '@/__test-utils__/react-flow/nodes.ts'
import { ReactFlowTesting } from '@/__test-utils__/react-flow/ReactFlowTesting.tsx'

import { FilterSelection } from '@/modules/Workspace/components/filters/index.ts'
import useWorkspaceStore from '@/modules/Workspace/hooks/useWorkspaceStore.ts'
import { NodeTypes } from '@/modules/Workspace/types.ts'

const getWrapperWith = (initialNodes?: Node[], initialEdges?: Edge[]) => {
  const Wrapper: JSXElementConstructor<{ children: React.ReactNode }> = ({ children }) => {
    const { nodes, onNodesChange } = useWorkspaceStore()

    const selectedNodes = useMemo(() => {
      return nodes.filter((e) => e.selected === true)
    }, [nodes])

    const handleChange = (nodeId: string) => (event: ChangeEvent<HTMLInputElement>) => {
      const node = nodes.find((f) => nodeId === f.id)
      if (node) {
        onNodesChange([{ id: node.id, type: 'select', selected: event.target.checked } as NodeSelectionChange])
      }
    }

    return (
      <ReactFlowTesting
        showDashboard={true}
        dashboard={
          <>
            <Text>
              <span data-testid="test-selected-length">{selectedNodes.length}</span>
              {' / '}
              <span data-testid="test-nodes-length">{nodes.length}</span>
            </Text>

            <VStack alignItems="flex-start">
              <CheckboxGroup>
                {nodes.map((node) => (
                  <Checkbox
                    key={node.id}
                    as={HStack}
                    onChange={handleChange(node.id)}
                    data-testid="test-node-selector"
                    isChecked={node.selected}
                  >
                    <Text>{node.id}</Text>
                  </Checkbox>
                ))}
              </CheckboxGroup>
            </VStack>
          </>
        }
        config={{
          initialState: {
            nodes: initialNodes,
            edges: initialEdges,
          },
        }}
      >
        {children}
      </ReactFlowTesting>
    )
  }

  return Wrapper
}

describe('FilterSelection', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
  })

  it('should render properly', () => {
    cy.mountWithProviders(<FilterSelection />, {
      wrapper: getWrapperWith([
        { ...MOCK_NODE_ADAPTER, position: { x: 50, y: 100 }, selected: false },
        { ...MOCK_NODE_DEVICE, position: { x: 50, y: 100 }, selected: false },
        { ...MOCK_NODE_COMBINER, position: { x: 50, y: 100 }, selected: false },
      ]),
    })

    cy.get('[role="group"] label#workspace-filter-selection-label').should('have.text', 'Selection')
    cy.get('[role="group"] #workspace-filter-selection-items').should('have.text', 'no entity selected')

    cy.getByTestId('workspace-filter-selection-add')
      .should('be.disabled')
      .should('have.attr', 'aria-label', 'Add selection')
    cy.getByTestId('workspace-filter-selection-clear')
      .should('be.disabled')
      .should('have.attr', 'aria-label', 'Clear selection')
  })

  it('should filter selected nodes', () => {
    const onChange = cy.stub().as('onChange')
    cy.mountWithProviders(<FilterSelection onChange={onChange} />, {
      wrapper: getWrapperWith([
        { ...MOCK_NODE_ADAPTER, position: { x: 50, y: 100 } },
        { ...MOCK_NODE_DEVICE, position: { x: 50, y: 100 } },
        { ...MOCK_NODE_COMBINER, position: { x: 50, y: 100 } },
      ]),
    })

    cy.get('@onChange').should('not.have.been.called')
    cy.getByTestId('test-selected-length').should('have.text', '0')
    cy.getByTestId('workspace-filter-selection-add').should('be.disabled')
    cy.getByTestId('workspace-filter-selection-clear').should('be.disabled')

    cy.getByTestId('test-node-selector').eq(0).click()

    cy.getByTestId('test-selected-length').should('have.text', '1')
    cy.get('[role="group"] #workspace-filter-selection-items').should('have.text', '1 entity selected')
    cy.getByTestId('workspace-filter-selection-add').should('not.be.disabled')

    cy.getByTestId('workspace-filter-selection-add').click()

    cy.get('@onChange').should('have.been.calledWith', [
      {
        id: 'idAdapter',
        type: 'ADAPTER_NODE',
      },
    ])

    cy.getByTestId('test-node-selector').eq(2).click()
    cy.get('[role="group"] #workspace-filter-selection-items').should('have.text', '2 entities selected')

    cy.getByTestId('workspace-filter-selection-add').click()
    cy.get('@onChange').should('have.been.calledWith', [
      {
        id: 'idAdapter',
        type: 'ADAPTER_NODE',
      },
      {
        id: 'idCombiner',
        type: 'COMBINER_NODE',
      },
    ])
  })

  it('should clear the filter', () => {
    const onChange = cy.stub().as('onChange')
    cy.mountWithProviders(
      <FilterSelection
        onChange={onChange}
        value={[
          {
            id: 'idAdapter',
            type: NodeTypes.ADAPTER_NODE,
          },
        ]}
      />,
      {
        wrapper: getWrapperWith([
          { ...MOCK_NODE_ADAPTER, position: { x: 50, y: 100 }, selected: true },
          { ...MOCK_NODE_DEVICE, position: { x: 50, y: 100 }, selected: false },
          { ...MOCK_NODE_COMBINER, position: { x: 50, y: 100 }, selected: false },
        ]),
      }
    )

    cy.get('@onChange').should('not.have.been.called')
    cy.getByTestId('test-selected-length').should('have.text', '1')
    cy.get('[role="group"] #workspace-filter-selection-items').should('have.text', '1 entity filtered')

    cy.getByTestId('workspace-filter-selection-add').should('not.be.disabled')
    cy.getByTestId('workspace-filter-selection-clear').should('not.be.disabled')

    cy.getByTestId('workspace-filter-selection-clear').click()
    cy.get('@onChange').should('have.been.calledWith', [])
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<FilterSelection />)

    cy.checkAccessibility()
  })

  it('should render properly when disabled', () => {
    const onChange = cy.stub().as('onChange')
    cy.mountWithProviders(<FilterSelection onChange={onChange} isDisabled />, {
      wrapper: getWrapperWith([
        { ...MOCK_NODE_ADAPTER, position: { x: 50, y: 100 }, selected: true },
        { ...MOCK_NODE_DEVICE, position: { x: 50, y: 100 }, selected: false },
        { ...MOCK_NODE_COMBINER, position: { x: 50, y: 100 }, selected: false },
      ]),
    })

    cy.get('[role="group"] ').should('have.attr', 'data-disabled')

    cy.getByTestId('workspace-filter-selection-add').should('be.disabled')
    cy.getByTestId('workspace-filter-selection-clear').should('be.disabled')

    cy.getByTestId('workspace-filter-selection-add').click({ force: true })
    cy.getByTestId('workspace-filter-selection-clear').click({ force: true })

    cy.get('@onChange').should('not.have.been.called')
  })
})
