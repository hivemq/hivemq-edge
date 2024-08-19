import { Checkbox, Stack, Text } from '@chakra-ui/react'
import { MOCK_NODE_ADAPTER, MOCK_NODE_BRIDGE } from '@/__test-utils__/react-flow/nodes.ts'
import { ReactFlowTesting } from '@/__test-utils__/react-flow/ReactFlowTesting.tsx'
import useWorkspaceStore from '@/modules/Workspace/hooks/useWorkspaceStore.ts'

import GroupNodesControl from '@/modules/Workspace/components/controls/GroupNodesControl.tsx'

const Wrapper: React.JSXElementConstructor<{ children: React.ReactNode; showDashboard?: boolean }> = ({
  children,
  showDashboard = true,
}) => {
  const { nodes, onNodesChange } = useWorkspaceStore()

  return (
    <ReactFlowTesting
      config={{
        initialState: {
          nodes: [
            { ...MOCK_NODE_ADAPTER, position: { x: 0, y: 0 } },
            { ...MOCK_NODE_ADAPTER, id: 'idAdapter2', position: { x: 0, y: 0 } },
            { ...MOCK_NODE_BRIDGE, position: { x: 0, y: 0 } },
          ],
        },
      }}
      showDashboard={showDashboard}
      dashboard={
        <Stack direction="column">
          {nodes.map((node) => (
            <Checkbox
              size="sm"
              key={node.id}
              isChecked={node.selected}
              onChange={() => {
                onNodesChange([{ id: node.id, type: 'select', selected: !node.selected }])
              }}
              data-testid="node-checked"
            >
              <Text as="span" data-testid="node-type">
                {node.type}
              </Text>{' '}
              <Text as="span" data-testid="node-group">
                {node.parentNode || node.parentId ? 'Grouped' : ''}
              </Text>{' '}
              <Text as="span" data-testid="node-id">
                ({node.id})
              </Text>
            </Checkbox>
          ))}
        </Stack>
      }
    >
      {children}
    </ReactFlowTesting>
  )
}

const WrapperLight: React.JSXElementConstructor<{ children: React.ReactNode }> = ({ children }) => (
  <Wrapper showDashboard={false}>{children}</Wrapper>
)

describe('GroupNodesControl', () => {
  beforeEach(() => {
    cy.viewport(500, 400)
  })

  it('should render properly', () => {
    cy.mountWithProviders(<GroupNodesControl />, { wrapper: Wrapper })

    cy.getByAriaLabel('Group the selected entities').should('be.disabled')
    cy.getByTestId('node-checked').should('have.length', 3)

    cy.getByTestId('node-checked').eq(0).should('not.have.attr', 'data-checked')
    cy.getByTestId('node-checked').eq(0).click()
    cy.getByTestId('node-checked').eq(0).should('have.attr', 'data-checked')
    cy.getByTestId('node-checked').eq(0).should('not.contain.text', 'Grouped')
    cy.getByTestId('node-checked').eq(2).click()
    cy.getByTestId('node-checked').eq(2).should('have.attr', 'data-checked')

    cy.getByAriaLabel('Group the selected entities').should('be.disabled')
    cy.getByTestId('node-checked').eq(1).click()
    cy.getByTestId('node-checked').eq(1).should('have.attr', 'data-checked')
    cy.getByTestId('node-checked').eq(1).should('not.contain.text', 'Grouped')

    cy.getByAriaLabel('Group the selected entities').should('not.be.disabled')

    cy.getByAriaLabel('Group the selected entities').click()

    cy.getByTestId('node-checked').should('have.length', 4)

    cy.getByTestId('node-checked').eq(0).should('contain.text', 'CLUSTER_NODE')
    cy.getByTestId('node-checked').eq(0).should('not.have.attr', 'data-checked')
    cy.getByTestId('node-checked').eq(1).should('not.have.attr', 'data-checked')
    cy.getByTestId('node-checked').eq(1).should('contain.text', 'Grouped')
    cy.getByTestId('node-checked').eq(2).should('not.have.attr', 'data-checked')
    cy.getByTestId('node-checked').eq(1).should('contain.text', 'Grouped')
    cy.getByTestId('node-checked').eq(3).should('have.attr', 'data-checked')
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<GroupNodesControl />, { wrapper: WrapperLight })

    cy.checkAccessibility()
    cy.percySnapshot('Component: GroupNodesControl')
  })
})
