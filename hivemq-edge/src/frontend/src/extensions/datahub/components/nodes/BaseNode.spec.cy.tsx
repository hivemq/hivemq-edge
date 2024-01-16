/// <reference types="cypress" />

import { NodeProps } from 'reactflow'

import { mockReactFlow } from '@/__test-utils__/react-flow/providers.tsx'
import { MOCK_DEFAULT_NODE } from '@/__test-utils__/react-flow/nodes.ts'

import { DataHubNodeType } from '../../types.ts'
import { BaseNode } from './BaseNode.tsx'

export const MOCK_NODE_ADAPTER: NodeProps<{ label: string }> = {
  id: 'idAdapter',
  type: DataHubNodeType.TOPIC_FILTER,
  data: { label: 'Hello1' },
  ...MOCK_DEFAULT_NODE,
}

describe('BaseNode', () => {
  beforeEach(() => {
    cy.viewport(400, 400)
  })

  it('should render properly', () => {
    cy.mountWithProviders(mockReactFlow(<BaseNode {...MOCK_NODE_ADAPTER} selected={true} />))
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(mockReactFlow(<BaseNode {...MOCK_NODE_ADAPTER} />))
    cy.checkAccessibility()
    cy.percySnapshot('Component: DataHub - BaseNode')
  })
})
