import type { Node } from 'reactflow'
import { MOCK_NODE_ADAPTER, MOCK_NODE_DEVICE, MOCK_NODE_GROUP } from '@/__test-utils__/react-flow/nodes.ts'
import { ReactFlowTesting } from '@/__test-utils__/react-flow/ReactFlowTesting.tsx'
import GroupContentEditor from '@/modules/Workspace/components/parts/GroupContentEditor.tsx'
import { mockAdapterConnectionStatus } from '@/api/hooks/useConnection/__handlers__'

const getWrapperWith = (initialNodes?: Node[]) => {
  const Wrapper: React.JSXElementConstructor<{ children: React.ReactNode }> = ({ children }) => {
    return (
      <ReactFlowTesting
        config={{
          initialState: {
            nodes: initialNodes,
          },
        }}
      >
        {children}
      </ReactFlowTesting>
    )
  }

  return Wrapper
}

describe('GroupContentEditor', () => {
  beforeEach(() => {
    cy.viewport(800, 600)
    cy.intercept('api/v1/management/protocol-adapters/status', { items: [mockAdapterConnectionStatus] }).as('getStatus')
  })

  it('should render properly', () => {
    cy.mountWithProviders(<GroupContentEditor group={{ ...MOCK_NODE_GROUP, position: { x: 50, y: 100 } }} />, {
      wrapper: getWrapperWith([
        { ...MOCK_NODE_ADAPTER, position: { x: 50, y: 100 } },
        { ...MOCK_NODE_DEVICE, id: 'idBridge', position: { x: 50, y: 100 } },
      ]),
    })

    cy.getByTestId('group-content-header').should('have.text', 'Content Management')
    cy.get('table').should('be.visible')
    cy.get('table thead tr th').should('have.length', 4)
    cy.get('table thead tr th').eq(0).should('have.text', 'Type')
    cy.get('table thead tr th').eq(1).should('have.text', 'Status')
    cy.get('table thead tr th').eq(2).should('have.text', 'Id')
    cy.get('table thead tr th').eq(3).should('have.text', 'Actions')
    cy.get('table tbody tr').should('have.length', 2)
    cy.get('table tbody tr').eq(0).find('td').as('adapter-row')
    cy.get('table tbody tr').eq(1).find('td').as('device-row')

    cy.get('@adapter-row').should('have.length', 4)
    cy.get('@adapter-row').eq(0).should('have.text', 'adapter')
    cy.get('@adapter-row').eq(1).should('have.text', 'Connected')
    cy.get('@adapter-row').eq(2).should('have.text', 'my-adapter')

    cy.get('@device-row').should('have.length', 4)
    cy.get('@device-row').eq(0).should('have.text', 'device')
    cy.get('@device-row').eq(1).should('have.text', 'Unknown')
    cy.get('@device-row').eq(2).should('have.text', 'simulation')

    cy.get('nav').should('be.visible').should('have.attr', 'aria-label', 'Pagination')
    cy.get('nav').find('[role="group"]').should('have.length', 2)
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<GroupContentEditor group={{ ...MOCK_NODE_GROUP, position: { x: 50, y: 100 } }} />, {
      wrapper: getWrapperWith([
        { ...MOCK_NODE_ADAPTER, position: { x: 50, y: 100 } },
        { ...MOCK_NODE_DEVICE, id: 'idBridge', position: { x: 50, y: 100 } },
      ]),
    })
    cy.checkAccessibility()
    cy.percySnapshot('Component: GroupContentEditor')
  })
})
