/// <reference types="cypress" />

import type { NodeProps, Node } from '@xyflow/react'

import { mockReactFlow } from '@/__test-utils__/react-flow/providers.tsx'
import { MOCK_DEFAULT_NODE } from '@/__test-utils__/react-flow/nodes.ts'

import type { TopicFilterData } from '@datahub/types.ts'
import { DataHubNodeType } from '@datahub/types.ts'
import { TopicFilterNode } from './TopicFilterNode.tsx'

export const MOCK_NODE_TOPIC_FILTER: NodeProps<Node<TopicFilterData>> = {
  id: 'topic-filter-id',
  type: DataHubNodeType.TOPIC_FILTER,
  data: { topics: ['topic 1', 'topic 2', 'topic 3'] },
  ...MOCK_DEFAULT_NODE,
}

describe('TopicFilterNode', () => {
  beforeEach(() => {
    cy.viewport(400, 400)
  })

  it('should render properly', () => {
    cy.mountWithProviders(mockReactFlow(<TopicFilterNode {...MOCK_NODE_TOPIC_FILTER} selected={true} />))
    cy.getByTestId(`node-title`).should('contain.text', 'Topic Filter')
    cy.getByTestId('topic-wrapper').should('have.length', 3)
    cy.getByTestId('topic-wrapper').eq(0).should('contain.text', 'topic 1')
    cy.getByTestId('topic-wrapper').eq(1).should('contain.text', 'topic 2')
    cy.getByTestId('topic-wrapper').eq(2).should('contain.text', 'topic 3')

    cy.get('div[data-handleid]').should('have.length', 3)
    cy.get('div[data-handleid]')
      .eq(0)
      .should('have.attr', 'data-id')
      .then((attr) => {
        expect((attr as unknown as string).endsWith('source')).to.be.true
      })
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(mockReactFlow(<TopicFilterNode {...MOCK_NODE_TOPIC_FILTER} />))
    cy.checkAccessibility()
    cy.percySnapshot('Component: DataHub - TopicFilterNode')
  })
})
