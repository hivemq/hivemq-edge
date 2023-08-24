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

    cy.getByTestId('edge-node-name').should('contain', 'HiveMQ Edge')
    cy.get('[data-handleid]').should('have.length', 3)
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(mockReactFlow(<NodeEdge {...MOCK_NODE_EDGE} />))
    cy.checkAccessibility(undefined, {
      rules: {
        // TODO[NVL] Font too small, creating accessibility issues. Need fix
        'color-contrast': { enabled: false },
      },
    })
    cy.percySnapshot('Component: NodeEdge')
  })
})
