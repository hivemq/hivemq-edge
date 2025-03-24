import { mockReactFlow } from '@/__test-utils__/react-flow/providers'
import { MOCK_NODE_COMBINER } from '@/__test-utils__/react-flow/nodes'
import { CustomNodeTesting } from '@/__test-utils__/react-flow/CustomNodeTesting'

import NodeCombiner from './NodeCombiner'
import { NodeTypes } from '../../types'

describe('NodeCombiner', () => {
  beforeEach(() => {
    cy.viewport(400, 400)
  })

  it('should render properly', () => {
    cy.mountWithProviders(mockReactFlow(<NodeCombiner {...MOCK_NODE_COMBINER} />))
    cy.getByTestId('combiner-description').should('have.text', 'my-combiner')
  })

  it('should render the selected combiner properly', () => {
    cy.mountWithProviders(
      <CustomNodeTesting
        nodes={[{ ...MOCK_NODE_COMBINER, position: { x: 90, y: 100 }, selected: true }]}
        nodeTypes={{ [NodeTypes.COMBINER_NODE]: NodeCombiner }}
      />
    )
    cy.getByTestId('combiner-description').should('have.text', 'my-combiner')

    cy.getByTestId('test-navigate-pathname').should('have.text', '/')
    cy.getByTestId('node-group-toolbar-panel').click()
    cy.getByTestId('test-navigate-pathname').should('have.text', `/workspace/combiner/${MOCK_NODE_COMBINER.id}`)
  })

  it('should render the mapping flags', () => {
    cy.mountWithProviders(
      <CustomNodeTesting
        nodes={[{ ...MOCK_NODE_COMBINER, position: { x: 90, y: 100 }, selected: true }]}
        nodeTypes={{ [NodeTypes.COMBINER_NODE]: NodeCombiner }}
      />
    )
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(mockReactFlow(<NodeCombiner {...MOCK_NODE_COMBINER} />))
    cy.checkAccessibility()
    cy.percySnapshot('Component: NodeCombiner')
  })
})
