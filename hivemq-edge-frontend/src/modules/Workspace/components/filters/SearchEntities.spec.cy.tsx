import type { Node } from '@xyflow/react'
import { MOCK_NODE_ADAPTER } from '@/__test-utils__/react-flow/nodes.ts'
import { mockReactFlow } from '@/__test-utils__/react-flow/providers.tsx'
import { ReactFlowTesting } from '@/__test-utils__/react-flow/ReactFlowTesting.tsx'
import SearchEntities from '@/modules/Workspace/components/filters/SearchEntities.tsx'
import useWorkspaceStore from '@/modules/Workspace/hooks/useWorkspaceStore.ts'
import { useMemo } from 'react'

const getWrapperWith = (initialNodes?: Node[]) => {
  const Wrapper: React.JSXElementConstructor<{ children: React.ReactNode }> = ({ children }) => {
    const { nodes } = useWorkspaceStore()
    const selected = useMemo(() => {
      return nodes.filter((node) => node.selected)
    }, [nodes])

    return (
      <ReactFlowTesting
        config={{
          initialState: {
            nodes: initialNodes,
          },
        }}
        showDashboard={true}
        dashboard={
          <>
            <div data-testid="data-length">{nodes.length}</div>
            <div data-testid="data-selected">{selected.length}</div>
          </>
        }
      >
        {children}
      </ReactFlowTesting>
    )
  }

  return Wrapper
}

describe('SearchEntities', () => {
  beforeEach(() => {
    cy.viewport(600, 400)
  })

  it('should render properly', () => {
    cy.mountWithProviders(mockReactFlow(<SearchEntities />))

    cy.get('[role="group"] > label').should('not.be.visible').should('have.text', 'Search for')
    cy.getByTestId('workspace-search').should('have.attr', 'placeholder', 'Search for ...')
    cy.getByTestId('workspace-search-clear').should('not.exist')
    cy.get('[role="group"] > [role="group"] ').within(() => {
      cy.getByTestId('workspace-search-prev').should('have.attr', 'aria-label', 'Previous entity').should('be.disabled')
      cy.getByTestId('workspace-search-next').should('have.attr', 'aria-label', 'Next entity').should('be.disabled')
      cy.getByTestId('workspace-search-counter').should('have.text', '0 of 0')
    })
  })

  it('should search', () => {
    const onchange = cy.stub().as('onchange')
    const onNavigate = cy.stub().as('onNavigate')
    cy.mountWithProviders(<SearchEntities onChange={onchange} onNavigate={onNavigate} />, {
      wrapper: getWrapperWith([
        { ...MOCK_NODE_ADAPTER, position: { x: 0, y: 0 } },
        { ...MOCK_NODE_ADAPTER, id: 'adapter2', position: { x: 0, y: 0 } },
      ]),
    })

    cy.get('@onchange').should('not.have.been.called')
    cy.get('@onNavigate').should('not.have.been.called')
    cy.getByTestId('workspace-search').type('test')
    cy.getByTestId('workspace-search').should('have.value', 'test')
    cy.getByTestId('workspace-search-clear').should('be.visible').should('have.attr', 'aria-label', 'Clear the search')
    cy.getByTestId('workspace-search-clear').click()
    cy.getByTestId('workspace-search').should('have.attr', 'placeholder', 'Search for ...')
    cy.getByTestId('workspace-search').should('have.value', '')
    cy.get('@onchange').should('have.been.calledWith', [])

    cy.getByTestId('workspace-search').type('adapt')
    cy.getByTestId('workspace-search-prev').should('not.be.disabled')
    cy.getByTestId('workspace-search-next').should('not.be.disabled')

    cy.get('@onchange').should('have.been.calledWith', ['idAdapter', 'adapter2'])

    cy.getByTestId('workspace-search-next').click()
    cy.get('@onNavigate').should('have.been.calledWith', 'adapter2')
    cy.getByTestId('workspace-search-next').click()
    cy.get('@onNavigate').should('have.been.calledWith', 'idAdapter')
    cy.getByTestId('workspace-search-prev').click()
    cy.get('@onNavigate').should('have.been.calledWith', 'adapter2')
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(mockReactFlow(<SearchEntities />))

    cy.checkAccessibility()
  })
})
