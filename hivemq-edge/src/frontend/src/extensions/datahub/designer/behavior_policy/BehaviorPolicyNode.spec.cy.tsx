/// <reference types="cypress" />

import type { NodeProps } from 'reactflow'

import { mockReactFlow } from '@/__test-utils__/react-flow/providers.tsx'
import { MOCK_DEFAULT_NODE } from '@/__test-utils__/react-flow/nodes.ts'

import type { BehaviorPolicyData } from '@datahub/types.ts'
import { BehaviorPolicyType, DataHubNodeType } from '@datahub/types.ts'
import { BehaviorPolicyNode } from '@datahub/designer/behavior_policy/BehaviorPolicyNode.tsx'

const MOCK_NODE_BEHAVIOR_POLICY: NodeProps<BehaviorPolicyData> = {
  id: 'node-id',
  type: DataHubNodeType.BEHAVIOR_POLICY,
  data: { id: 'my-policy-id', model: BehaviorPolicyType.MQTT_EVENT },
  ...MOCK_DEFAULT_NODE,
}

describe('BehaviorPolicyNode', () => {
  beforeEach(() => {
    cy.viewport(400, 400)
  })

  it('should render properly', () => {
    cy.mountWithProviders(mockReactFlow(<BehaviorPolicyNode {...MOCK_NODE_BEHAVIOR_POLICY} selected={true} />))
    cy.getByTestId(`node-title`).should('contain.text', 'Behavior Policy')
    cy.getByTestId(`node-model`).should('contain.text', 'Mqtt.events')

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
    cy.mountWithProviders(mockReactFlow(<BehaviorPolicyNode {...MOCK_NODE_BEHAVIOR_POLICY} />))
    cy.checkAccessibility()
    cy.percySnapshot('Component: DataHub - BehaviorPolicyNode')
  })
})
