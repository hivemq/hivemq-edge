/// <reference types="cypress" />

import { MOCK_NODE_EDGE } from '@/__test-utils__/react-flow/nodes.ts'
import { mockReactFlow } from '@/__test-utils__/react-flow/providers.tsx'

import NodeEdge from './NodeEdge.tsx'
import { mockProtocolAdapter } from '@/api/hooks/useProtocolAdapters/__handlers__'
import { MOCK_TOPIC_FILTER } from '@/api/hooks/useTopicFilters/__handlers__'
import { formatTopicString } from '../../../../components/MQTT/topic-utils'

describe('NodeEdge', () => {
  beforeEach(() => {
    cy.viewport(400, 400)
  })

  it('should render properly', () => {
    cy.intercept('/api/v1/management/protocol-adapters/types', { items: [mockProtocolAdapter] }).as('getTypes')
    cy.intercept('/api/v1/management/topic-filters', {
      items: [
        MOCK_TOPIC_FILTER,
        {
          topicFilter: 'another/filter',
          description: 'This is a topic filter',
        },
        {
          topicFilter: 'another/filter/too',
          description: 'This is another topic filter',
        },
      ],
    }).as('getTopicFilters')

    cy.mountWithProviders(mockReactFlow(<NodeEdge {...MOCK_NODE_EDGE} />))

    cy.getByTestId('edge-node-icon').should('have.attr', 'alt', 'Node: HiveMQ Edge')
    cy.get('[data-handleid]').should('have.length', 4)

    cy.getByTestId('edge-node-title').should('have.text', 'HiveMQ Edge')
    cy.getByTestId('topics-container').within(() => {
      cy.getByTestId('topic-wrapper').eq(0).should('have.text', formatTopicString('a/topic/+/filter'))
      cy.getByTestId('topic-wrapper').eq(1).should('have.text', formatTopicString('another/filter'))
      cy.getByTestId('topics-show-more').should('have.text', '+1')
    })
  })

  it('should be accessible', () => {
    cy.intercept('/api/v1/management/protocol-adapters/types', { items: [mockProtocolAdapter] }).as('getTypes')
    cy.intercept('/api/v1/management/topic-filters', {
      items: [MOCK_TOPIC_FILTER],
    }).as('getTopicFilters')
    cy.injectAxe()
    cy.mountWithProviders(mockReactFlow(<NodeEdge {...MOCK_NODE_EDGE} />))
    cy.checkAccessibility()
    cy.percySnapshot('Component: NodeEdge')
  })
})
