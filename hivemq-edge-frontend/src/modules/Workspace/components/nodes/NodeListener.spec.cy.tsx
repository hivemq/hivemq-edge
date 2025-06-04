/// <reference types="cypress" />

import { MOCK_NODE_LISTENER } from '@/__test-utils__/react-flow/nodes.ts'
import { mockReactFlow } from '@/__test-utils__/react-flow/providers.tsx'

import NodeListener from '@/modules/Workspace/components/nodes/NodeListener.tsx'

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
    cy.checkAccessibility()
    cy.percySnapshot('Component: NodeListener')
  })
})
