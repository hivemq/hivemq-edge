/// <reference types="cypress" />

import { NodeProps } from 'reactflow'

import { mockReactFlow } from '@/__test-utils__/react-flow/providers.tsx'
import { MOCK_DEFAULT_NODE } from '@/__test-utils__/react-flow/nodes.ts'

import { DataHubNodeType, FunctionData } from '../../types.ts'
import { FunctionNode } from './FunctionNode.tsx'

const MOCK_NODE_FUNCTION: NodeProps<FunctionData> = {
  id: 'node-id',
  type: DataHubNodeType.FUNCTION,
  data: {
    type: 'Javascript',
    name: 'string',
    version: 1,
    sourceCode: 'string',
  },
  ...MOCK_DEFAULT_NODE,
}

describe('FunctionNode', () => {
  beforeEach(() => {
    cy.viewport(400, 400)
  })

  it('should render properly', () => {
    cy.mountWithProviders(mockReactFlow(<FunctionNode {...MOCK_NODE_FUNCTION} selected={true} />))
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(mockReactFlow(<FunctionNode {...MOCK_NODE_FUNCTION} />))
    cy.checkAccessibility()
    cy.percySnapshot('Component: DataHub - FunctionNode')
  })
})
