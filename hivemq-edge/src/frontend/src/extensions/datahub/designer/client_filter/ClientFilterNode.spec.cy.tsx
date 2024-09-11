/// <reference types="cypress" />

import { NodeProps } from 'reactflow'

import { mockReactFlow } from '@/__test-utils__/react-flow/providers.tsx'
import { MOCK_DEFAULT_NODE } from '@/__test-utils__/react-flow/nodes.ts'

import { ClientFilterData, DataHubNodeType } from '@datahub/types.ts'
import { ClientFilterNode } from './ClientFilterNode.tsx'

const MOCK_NODE_CLIENT_FILTER: NodeProps<ClientFilterData> = {
  id: 'node-id',
  type: DataHubNodeType.CLIENT_FILTER,
  data: { clients: ['client1', 'client2'] },
  ...MOCK_DEFAULT_NODE,
}

describe('', () => {
  beforeEach(() => {
    cy.viewport(400, 400)
  })

  it('should render properly', () => {
    cy.mountWithProviders(mockReactFlow(<ClientFilterNode {...MOCK_NODE_CLIENT_FILTER} selected={true} />))
    cy.getByTestId(`node-title`).should('contain.text', 'Client Filter')
    cy.getByTestId('client-wrapper').should('have.length', 2)
    cy.getByTestId('client-wrapper').eq(0).should('contain.text', 'client1')
    cy.getByTestId('client-wrapper').eq(1).should('contain.text', 'client2')

    // TODO[NVL] Create a PageObject and generalise the selectors
    cy.get('div[data-handleid]').should('have.length', 2)
    cy.get('div[data-handleid]')
      .eq(0)
      .should('have.attr', 'data-id')
      .then((attr) => {
        expect((attr as unknown as string).endsWith('source')).to.be.true
      })
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(mockReactFlow(<ClientFilterNode {...MOCK_NODE_CLIENT_FILTER} />))
    cy.checkAccessibility()
    cy.percySnapshot('Component: DataHub - ClientFilterNode')
  })
})
