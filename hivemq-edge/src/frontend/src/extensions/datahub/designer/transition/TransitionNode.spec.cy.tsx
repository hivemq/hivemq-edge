/// <reference types="cypress" />

import type { NodeProps } from 'reactflow'

import { mockReactFlow } from '@/__test-utils__/react-flow/providers.tsx'
import { MOCK_DEFAULT_NODE } from '@/__test-utils__/react-flow/nodes.ts'

import type { TransitionData } from '@datahub/types.ts'
import { DataHubNodeType } from '@datahub/types.ts'
import { TransitionNode } from './TransitionNode.tsx'

const MOCK_NODE_TRANSITION: NodeProps<TransitionData> = {
  id: 'node-id',
  type: DataHubNodeType.TRANSITION,
  data: {},
  ...MOCK_DEFAULT_NODE,
}

describe('TransitionNode', () => {
  beforeEach(() => {
    cy.viewport(400, 400)
  })

  it('should render properly', () => {
    cy.mountWithProviders(mockReactFlow(<TransitionNode {...MOCK_NODE_TRANSITION} selected={true} />))
    cy.getByTestId(`node-title`).should('contain.text', 'Transition')
    cy.getByTestId(`node-model`).should('contain.text', '< not set >')

    // TODO[NVL] Create a PageObject and generalise the selectors
    cy.get('div[data-handleid]').should('have.length', 2)
    cy.get('div[data-handleid]')
      .eq(0)
      .should('have.attr', 'data-handlepos', 'left')
      .should('have.attr', 'data-id')
      .then((attr) => {
        expect((attr as unknown as string).endsWith('target')).to.be.true
      })

    cy.get('div[data-handleid]')
      .eq(1)
      .should('have.attr', 'data-handlepos', 'right')
      .should('have.attr', 'data-id')
      .then((attr) => {
        expect((attr as unknown as string).endsWith('source')).to.be.true
      })
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(mockReactFlow(<TransitionNode {...MOCK_NODE_TRANSITION} />))
    cy.checkAccessibility()
    cy.percySnapshot('Component: DataHub - TransitionNode')
  })
})
