import { Route, Routes } from 'react-router-dom'
import type { Node } from 'reactflow'

import { ReactFlowTesting } from '@/__test-utils__/react-flow/ReactFlowTesting.tsx'
import { MOCK_NODE_COMBINER } from '@/__test-utils__/react-flow/nodes.ts'
import { NodeTypes } from '@/modules/Workspace/types'
import useWorkspaceStore from '@/modules/Workspace/hooks/useWorkspaceStore.ts'
import CombinerMappingManager from './CombinerMappingManager'

const getWrapperWith = (initialNodes?: Node[]) => {
  const Wrapper: React.JSXElementConstructor<{ children: React.ReactNode }> = ({ children }) => {
    const { nodes } = useWorkspaceStore()
    return (
      <ReactFlowTesting
        config={{
          initialState: {
            nodes: initialNodes,
          },
        }}
        showDashboard={true}
        dashboard={<div data-testid="data-length">{nodes.length}</div>}
      >
        <Routes>
          <Route path="/node/:combinerId" element={children}></Route>
        </Routes>
      </ReactFlowTesting>
    )
  }

  return Wrapper
}

describe('CombinerMappingManager', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
  })

  it('should render the drawer', () => {
    cy.mountWithProviders(<CombinerMappingManager />, {
      routerProps: { initialEntries: [`/node/wrong-adapter`] },
      wrapper: getWrapperWith(),
    })

    cy.get('[role="dialog"]').should('be.visible')

    cy.get('header').should('contain.text', 'Manage Data combining mappings')
    cy.get('[role="dialog"]').find('button').as('dialog-buttons').should('have.length', 1)
    cy.get('@dialog-buttons').eq(0).should('have.attr', 'aria-label', 'Close')

    cy.get('@dialog-buttons').eq(0).click()
    cy.get('[role="dialog"]').should('not.exist')
  })

  it('should render error properly', () => {
    cy.mountWithProviders(<CombinerMappingManager />, {
      routerProps: { initialEntries: [`/node/wrong-adapter`] },
      wrapper: getWrapperWith(),
    })

    cy.get('[role="dialog"]').should('be.visible')

    cy.get('[role="alert"]').should('be.visible')
    cy.get('[role="alert"] span').should('have.attr', 'data-status', 'error')
    cy.get('[role="alert"] div div')
      .should('have.attr', 'data-status', 'error')
      .should('contain.text', 'There was a problem loading the data')
  })

  it('should render data combining properly', () => {
    cy.mountWithProviders(<CombinerMappingManager />, {
      routerProps: { initialEntries: [`/node/idCombiner`] },
      wrapper: getWrapperWith([{ ...MOCK_NODE_COMBINER, position: { x: 0, y: 0 } }]),
    })
    cy.get('header').should('contain.text', 'Manage Data combining mappings')

    cy.get('[role="alert"]').should('not.exist')

    cy.getByTestId('node-type-icon').should('exist').should('have.attr', 'data-nodeicon', NodeTypes.COMBINER_NODE)
    cy.getByTestId('node-name').should('contain.text', 'my-combiner')
    cy.getByTestId('node-description').should('contain.text', 'Data Combiner')

    cy.get('[role="tablist"] [role="tab"]').should('have.length', 3)
    cy.get('[role="tablist"] [role="tab"]').eq(0).should('have.text', 'Configuration')
    cy.get('[role="tablist"] [role="tab"]').eq(1).should('have.text', 'Sources')
    cy.get('[role="tablist"] [role="tab"]').eq(2).should('have.text', 'Mappings')

    // TODO[NVL] More tests. But we need a strategy for testing OpenAPI/JSONSchema/RJSF forms
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<CombinerMappingManager />, {
      routerProps: { initialEntries: [`/node/idCombiner`] },
      wrapper: getWrapperWith([{ ...MOCK_NODE_COMBINER, position: { x: 0, y: 0 } }]),
    })

    cy.checkAccessibility()

    cy.get('[role="tablist"] [role="tab"]').eq(0).should('have.text', 'Configuration')
    cy.checkAccessibility()

    cy.get('[role="tablist"] [role="tab"]').eq(1).should('have.text', 'Sources')
    cy.get('[role="tablist"] [role="tab"]').eq(1).click()
    cy.checkAccessibility()

    cy.get('[role="tablist"] [role="tab"]').eq(2).should('have.text', 'Mappings')
    cy.get('[role="tablist"] [role="tab"]').eq(2).click()
    cy.checkAccessibility()
  })
})
