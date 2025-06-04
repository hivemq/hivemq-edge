import type { NodeProps, Node } from '@xyflow/react'

import { mockReactFlow } from '@/__test-utils__/react-flow/providers.tsx'
import { MOCK_DEFAULT_NODE } from '@/__test-utils__/react-flow/nodes.ts'

import type { DataPolicyData } from '@datahub/types.ts'
import { DataHubNodeType } from '@datahub/types.ts'
import { DataPolicyNode } from './DataPolicyNode.tsx'

const MOCK_NODE_DATA_POLICY: NodeProps<Node<DataPolicyData>> = {
  id: 'node-id',
  type: DataHubNodeType.DATA_POLICY,
  data: { id: 'my-policy-id' },
  ...MOCK_DEFAULT_NODE,
}

describe('DataPolicyNode', () => {
  beforeEach(() => {
    cy.viewport(400, 400)
  })

  it('should render properly', () => {
    cy.mountWithProviders(mockReactFlow(<DataPolicyNode {...MOCK_NODE_DATA_POLICY} selected={true} />))
    cy.getByTestId(`node-title`).should('contain.text', 'Data Policy')
    cy.getByTestId(`node-model`).find('p').eq(0).should('contain.text', 'filter')
    cy.getByTestId(`node-model`).find('p').eq(1).should('contain.text', 'validation')
    cy.getByTestId(`node-model`).find('p').eq(2).should('contain.text', 'onSuccess')
    cy.getByTestId(`node-model`).find('p').eq(3).should('contain.text', 'onError')

    // TODO[NVL] Create a PageObject and generalise the selectors
    cy.get('div[data-handleid]').should('have.length', 4)
    cy.get('div[data-handleid]')
      .eq(0)
      .should('have.attr', 'data-handlepos', 'left')
      .should('have.attr', 'data-id')
      .then((attr) => {
        expect((attr as unknown as string).endsWith('target')).to.be.true
      })
    cy.get('div[data-handleid]')
      .eq(1)
      .should('have.attr', 'data-handlepos', 'left')
      .should('have.attr', 'data-id')
      .then((attr) => {
        expect((attr as unknown as string).endsWith('target')).to.be.true
      })

    cy.get('div[data-handleid]')
      .eq(2)
      .should('have.attr', 'data-handlepos', 'right')
      .should('have.attr', 'data-handleid', 'onSuccess')
      .should('have.attr', 'data-id')
      .then((attr) => {
        expect((attr as unknown as string).endsWith('source')).to.be.true
      })

    cy.get('div[data-handleid]')
      .eq(3)
      .should('have.attr', 'data-handlepos', 'right')
      .should('have.attr', 'data-handleid', 'onError')
      .should('have.attr', 'data-id')
      .then((attr) => {
        expect((attr as unknown as string).endsWith('source')).to.be.true
      })
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(mockReactFlow(<DataPolicyNode {...MOCK_NODE_DATA_POLICY} />))
    cy.checkAccessibility()
    cy.percySnapshot('Component: DataHub - BehaviorPolicyNode')
  })
})
