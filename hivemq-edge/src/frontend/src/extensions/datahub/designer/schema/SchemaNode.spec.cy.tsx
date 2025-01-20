/// <reference types="cypress" />

import { NodeProps } from 'reactflow'

import { mockReactFlow } from '@/__test-utils__/react-flow/providers.tsx'
import { MOCK_DEFAULT_NODE } from '@/__test-utils__/react-flow/nodes.ts'

import { DataHubNodeType, SchemaData, SchemaType } from '@datahub/types.ts'
import { SchemaNode } from './SchemaNode.tsx'

const MOCK_NODE_SCHEMA: NodeProps<SchemaData> = {
  id: 'node-id',
  type: DataHubNodeType.SCHEMA,
  data: { type: SchemaType.JSON, version: 1, name: 'node-id' },
  ...MOCK_DEFAULT_NODE,
}

describe('OperationNode', () => {
  beforeEach(() => {
    cy.viewport(400, 400)
  })

  it('should render properly', () => {
    cy.mountWithProviders(mockReactFlow(<SchemaNode {...MOCK_NODE_SCHEMA} selected={true} />))
    cy.getByTestId(`node-title`).should('contain.text', 'Schema')
    cy.getByTestId(`node-model`).should('contain.text', 'JSON')

    // TODO[NVL] Create a PageObject and generalise the selectors
    cy.get('div[data-handleid]').should('have.length', 1)
    cy.get('div[data-handleid]')
      .eq(0)
      .should('have.attr', 'data-handlepos', 'right')
      .should('have.attr', 'data-id')
      .then((attr) => {
        expect((attr as unknown as string).endsWith('source')).to.be.true
      })
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(mockReactFlow(<SchemaNode {...MOCK_NODE_SCHEMA} />))
    cy.checkAccessibility()
    cy.percySnapshot('Component: DataHub - SchemaNode')
  })
})
