/// <reference types="cypress" />

import { MOCK_NODE_EDGE } from '@/__test-utils__/react-flow/nodes.ts'
import { mockReactFlow } from '@/__test-utils__/react-flow/providers.tsx'

import NodeEdge from './NodeEdge.tsx'

describe('NodeEdge', () => {
  beforeEach(() => {
    cy.viewport(400, 400)
  })

  it('should render properly', () => {
    cy.mountWithProviders(mockReactFlow(<NodeEdge {...MOCK_NODE_EDGE} />))

    cy.getByTestId('edge-node').should('have.attr', 'alt', 'Node: HiveMQ Edge')
    cy.get('[data-handleid]').should('have.length', 3)
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(mockReactFlow(<NodeEdge {...MOCK_NODE_EDGE} />))
    cy.checkAccessibility()
    cy.percySnapshot('Component: NodeEdge')
  })
})
