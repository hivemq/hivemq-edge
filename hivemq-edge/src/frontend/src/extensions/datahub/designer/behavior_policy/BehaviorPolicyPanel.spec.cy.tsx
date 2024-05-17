/// <reference types="cypress" />

import { Button } from '@chakra-ui/react'

import { MockStoreWrapper } from '@datahub/__test-utils__/MockStoreWrapper.tsx'
import { mockBehaviorPolicy } from '@datahub/api/hooks/DataHubBehaviorPoliciesService/__handlers__'
import { BehaviorPolicyPanel } from '@datahub/designer/behavior_policy/BehaviorPolicyPanel.tsx'
import { DataHubNodeType } from '@datahub/types.ts'
import { getNodePayload } from '@datahub/utils/node.utils.ts'

const wrapper: React.JSXElementConstructor<{ children: React.ReactNode }> = ({ children }) => (
  <MockStoreWrapper
    config={{
      initialState: {
        nodes: [
          {
            id: '3',
            type: DataHubNodeType.BEHAVIOR_POLICY,
            position: { x: 0, y: 0 },
            data: getNodePayload(DataHubNodeType.BEHAVIOR_POLICY),
          },
        ],
      },
    }}
  >
    {children}
    <Button variant="primary" type="submit" form="datahub-node-form">
      SUBMIT{' '}
    </Button>
  </MockStoreWrapper>
)

describe('BehaviorPolicyPanel', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
    cy.intercept('/api/v1/data-hub/behavior-validation/policies', {
      items: [mockBehaviorPolicy],
    })
  })

  it('should render the fields for the panel', () => {
    const onSubmit = cy.stub().as('onSubmit')

    cy.mountWithProviders(<BehaviorPolicyPanel selectedNode="3" onFormSubmit={onSubmit} />, { wrapper })

    cy.get('#root_id').type('a123')

    cy.get('label#root_model-label').should('contain.text', 'Behavior Model')
    cy.get('label#root_model-label + div').should('contain.text', 'Mqtt.events')
    cy.get('label#root_model-label + div').click()
    cy.get('label#root_model-label + div')
      .find("[role='listbox']")
      .find("[role='option']")
      .eq(1)
      .should('contain.text', 'Mqtt.events')
    cy.get('label#root_model-label + div')
      .find("[role='listbox']")
      .find("[role='option']")
      .eq(0)
      .should('contain.text', 'Publish.quota')
    cy.get('label#root_model-label + div').find("[role='listbox']").find("[role='option']").eq(0).click()

    cy.get('h2').eq(0).should('contain.text', 'Publish')
    cy.get('label#field-\\:r9\\:-label').should('contain.text', 'minPublishes')
    cy.get('label#field-\\:r9\\:-label + div > input').should('have.value', '0')
    cy.get('label#field-\\:rb\\:-label').should('contain.text', 'maxPublishes')
    cy.get('label#field-\\:rb\\:-label + div > input').should('have.value', '-1')

    cy.get("button[type='submit']").click()
    cy.get('@onSubmit')
      .should('have.been.calledOnceWith', Cypress.sinon.match.object)
      .its('firstCall.args.0')
      .should('deep.include', {
        status: 'submitted',
        formData: {
          id: 'a123',
          model: 'Publish.quota',
          arguments: {
            minPublishes: 0,
            maxPublishes: -1,
          },
        },
      })
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<BehaviorPolicyPanel selectedNode="3" />, { wrapper })

    cy.checkAccessibility()
    cy.percySnapshot('Component: BehaviorPolicyPanel')
  })
})
