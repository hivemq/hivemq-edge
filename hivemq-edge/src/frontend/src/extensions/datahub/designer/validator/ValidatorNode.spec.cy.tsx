/// <reference types="cypress" />

import type { NodeProps } from 'reactflow'

import { mockReactFlow } from '@/__test-utils__/react-flow/providers.tsx'
import { MOCK_DEFAULT_NODE } from '@/__test-utils__/react-flow/nodes.ts'

import { DataPolicyValidator } from '@/api/__generated__'
import type { ValidatorData } from '@datahub/types.ts'
import { DataHubNodeType, StrategyType } from '@datahub/types.ts'
import { ValidatorNode } from './ValidatorNode.tsx'

const MOCK_NODE_VALIDATOR: NodeProps<ValidatorData> = {
  id: 'node-id',
  type: DataHubNodeType.VALIDATOR,
  data: { type: DataPolicyValidator.type.SCHEMA, strategy: StrategyType.ALL_OF, schemas: [] },
  ...MOCK_DEFAULT_NODE,
}

describe('ValidatorNode', () => {
  beforeEach(() => {
    cy.viewport(400, 400)
  })

  it('should render properly', () => {
    cy.mountWithProviders(mockReactFlow(<ValidatorNode {...MOCK_NODE_VALIDATOR} selected={true} />))
    cy.getByTestId(`node-title`).should('contain.text', 'Policy Validator')
    cy.getByTestId(`node-model`).find('p').should('have.length', 2)
    cy.getByTestId(`node-model`).find('p').eq(0).should('contain.text', 'SCHEMA')
    cy.getByTestId(`node-model`).find('p').eq(1).should('contain.text', 'ALL_OF')

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
    cy.mountWithProviders(mockReactFlow(<ValidatorNode {...MOCK_NODE_VALIDATOR} />))
    cy.checkAccessibility()
    cy.percySnapshot('Component: DataHub - ValidatorNode')
  })
})
