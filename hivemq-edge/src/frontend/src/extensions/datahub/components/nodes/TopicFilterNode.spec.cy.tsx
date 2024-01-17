/// <reference types="cypress" />

import { NodeProps } from 'reactflow'

import { mockReactFlow } from '@/__test-utils__/react-flow/providers.tsx'
import { MOCK_DEFAULT_NODE } from '@/__test-utils__/react-flow/nodes.ts'

import { DataHubNodeType, TopicFilterData } from '../../types.ts'
import { TopicFilterNode } from './TopicFilterNode.tsx'

export const MOCK_NODE_ADAPTER: NodeProps<TopicFilterData> = {
  id: 'idAdapter',
  type: DataHubNodeType.TOPIC_FILTER,
  data: { topics: ['topic 1', 'Topic 2', 'topic 3'] },
  ...MOCK_DEFAULT_NODE,
}

describe('TopicFilterNode', () => {
  beforeEach(() => {
    cy.viewport(400, 400)
  })

  it('should render properly', () => {
    cy.mountWithProviders(mockReactFlow(<TopicFilterNode {...MOCK_NODE_ADAPTER} selected={true} />))
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(mockReactFlow(<TopicFilterNode {...MOCK_NODE_ADAPTER} />))
    cy.checkAccessibility()
    cy.percySnapshot('Component: DataHub - BaseNode')
  })
})
