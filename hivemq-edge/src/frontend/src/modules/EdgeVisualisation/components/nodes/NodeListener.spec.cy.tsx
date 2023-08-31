/// <reference types="cypress" />

import { MOCK_NODE_LISTENER } from '@/__test-utils__/react-flow/nodes.ts'
import { mockReactFlow } from '@/__test-utils__/react-flow/providers.tsx'

import NodeListener from '@/modules/EdgeVisualisation/components/nodes/NodeListener.tsx'

describe('NodeListener', () => {
  beforeEach(() => {
    cy.viewport(400, 400)
  })

  it('should render properly', () => {
    cy.mountWithProviders(mockReactFlow(<NodeListener {...MOCK_NODE_LISTENER} />))

    cy.get('[data-handleid]').should('have.length', 1)
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(mockReactFlow(<NodeListener {...MOCK_NODE_LISTENER} />))
    cy.checkAccessibility(undefined, {
      rules: {
        // TODO[16486] Font too small. See https://hivemq.kanbanize.com/ctrl_board/57/cards/16486/details/
        'color-contrast': { enabled: false },
      },
    })
    cy.percySnapshot('Component: NodeListener')
  })
})
