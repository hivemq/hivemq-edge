import type { JSXElementConstructor } from 'react'
import type { Edge, Node } from '@xyflow/react'

import { MOCK_NODE_ADAPTER, MOCK_NODE_COMBINER, MOCK_NODE_DEVICE } from '@/__test-utils__/react-flow/nodes.ts'
import { ReactFlowTesting } from '@/__test-utils__/react-flow/ReactFlowTesting.tsx'
import { FilterEntities } from '@/modules/Workspace/components/filters/index.ts'

const getWrapperWith = (initialNodes?: Node[], initialEdges?: Edge[]) => {
  const Wrapper: JSXElementConstructor<{ children: React.ReactNode }> = ({ children }) => {
    return (
      <ReactFlowTesting
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

describe('FilterEntities', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
  })

  it('should render properly', () => {
    const onChange = cy.stub().as('onChange')

    cy.mountWithProviders(<FilterEntities onChange={onChange} />, {
      wrapper: getWrapperWith([
        { ...MOCK_NODE_ADAPTER, position: { x: 50, y: 100 } },
        { ...MOCK_NODE_DEVICE, position: { x: 50, y: 100 } },
        { ...MOCK_NODE_COMBINER, position: { x: 50, y: 100 } },
      ]),
    })

    cy.get('[role="group"] label#workspace-filter-entities-label').should('have.text', 'Entities')
    cy.get('[role="group"] #react-select-entities-placeholder').should('have.text', 'Select entities to filter ...')
    cy.get('[role="group"] #workspace-filter-entities-trigger').click()
    cy.get('#react-select-entities-listbox [role="listbox"]').within(() => {
      cy.get('[role="option"]').should('have.length', 3)
      cy.get('[role="option"]').eq(0).should('have.text', 'Adapters')
      cy.get('[role="option"]').eq(1).should('have.text', 'Devices')
      cy.get('[role="option"]').eq(2).should('have.text', 'Combiners')
    })

    cy.get('[role="group"] #workspace-filter-entities-trigger').type('adapt{enter}')
    cy.getByTestId('workspace-filter-entities-values').should('have.length', 1)
    cy.getByTestId('workspace-filter-entities-values').eq(0).should('have.text', 'Adapters')

    cy.get('@onChange').should('have.been.calledWith', [
      {
        label: 'Adapters',
        value: 'ADAPTER_NODE',
      },
    ])

    cy.get('[role="group"] #workspace-filter-entities-trigger').type('comb{enter}')
    cy.getByTestId('workspace-filter-entities-values').should('have.length', 2)
    cy.getByTestId('workspace-filter-entities-values').eq(0).should('have.text', 'Adapters')
    cy.getByTestId('workspace-filter-entities-values').eq(1).should('have.text', 'Combiners')
    cy.get('@onChange').should('have.been.calledWith', [
      {
        label: 'Adapters',
        value: 'ADAPTER_NODE',
      },
      {
        label: 'Combiners',
        value: 'COMBINER_NODE',
      },
    ])

    cy.getByAriaLabel('Clear selected options').click()
    cy.getByTestId('workspace-filter-entities-values').should('have.length', 0)
    cy.get('@onChange').should('have.been.calledWith', [])
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<FilterEntities />)

    cy.checkAccessibility()
  })

  it('should render properly when disabled', () => {
    const onChange = cy.stub().as('onChange')

    cy.mountWithProviders(<FilterEntities onChange={onChange} isDisabled />, {
      wrapper: getWrapperWith([
        { ...MOCK_NODE_ADAPTER, position: { x: 50, y: 100 } },
        { ...MOCK_NODE_DEVICE, position: { x: 50, y: 100 } },
        { ...MOCK_NODE_COMBINER, position: { x: 50, y: 100 } },
      ]),
    })

    cy.get('[role="group"] label#workspace-filter-entities-label').should('have.text', 'Entities')
    cy.get('[role="group"] #workspace-filter-entities-trigger').should('have.attr', 'aria-disabled', 'true')

    cy.get('[role="group"] #workspace-filter-entities-trigger').click({ force: true })
    cy.get('#react-select-entities-listbox [role="listbox"]').should('not.exist')

    cy.get('@onChange').should('not.have.been.called')
  })
})
