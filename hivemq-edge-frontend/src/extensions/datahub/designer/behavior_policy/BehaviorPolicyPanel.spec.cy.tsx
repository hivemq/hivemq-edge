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
      SUBMIT
    </Button>
  </MockStoreWrapper>
)

const cy_selectBehaviourModel = (index: number) => {
  cy.get('label#root_model-label + div').click()

  cy.get('label#root_model-label + div').find("[role='listbox']").find("[role='option']").eq(index).click()
}

describe('BehaviorPolicyPanel', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
    cy.intercept('/api/v1/data-hub/behavior-validation/policies', {
      items: [mockBehaviorPolicy],
    }).as('getNodePayload')
  })

  it('should render loading and error states', () => {
    const onFormError = cy.stub().as('onFormError')
    cy.intercept('/api/v1/data-hub/behavior-validation/policies', { statusCode: 404 }).as('getPolicies')

    cy.mountWithProviders(<BehaviorPolicyPanel selectedNode="3" onFormError={onFormError} />, {
      wrapper,
    })
    cy.getByTestId('loading-spinner').should('be.visible')

    cy.wait('@getPolicies')
    cy.get('[role="alert"]')
      .should('be.visible')
      .should('have.attr', 'data-status', 'error')
      .should('have.text', 'Not Found')

    cy.get('@onFormError').should('have.been.calledWithErrorMessage', 'Not Found')
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
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<BehaviorPolicyPanel selectedNode="3" />, { wrapper })

    cy.checkAccessibility()
    cy.percySnapshot('Component: BehaviorPolicyPanel')
  })

  context('Behaviour models', () => {
    it('should render the Quota options', () => {
      const onSubmit = cy.stub().as('onSubmit')

      cy.mountWithProviders(<BehaviorPolicyPanel selectedNode="3" onFormSubmit={onSubmit} />, { wrapper })
      cy.get('#root_id').type('a123')

      cy_selectBehaviourModel(0)

      cy.get('h2').eq(0).should('contain.text', 'Publish.quota options')
      cy.get('label[for="root_arguments_minPublishes"]').should('contain.text', 'minPublishes')
      cy.get('label[for="root_arguments_minPublishes"] + div > input').should('have.value', '0')
      cy.get('label[for="root_arguments_maxPublishes"]').should('contain.text', 'maxPublishes')
      cy.get('label[for="root_arguments_maxPublishes"] + div > input').should('have.value', '-1')

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

    it('should render id errors', () => {
      cy.mountWithProviders(<BehaviorPolicyPanel selectedNode="3" onFormSubmit={cy.stub} />, { wrapper })
      cy.get("button[type='submit']").click()

      cy.get('[role="group"]:has(> label#root_id-label) + ul > li').as('idErrors')
      cy.get('@idErrors').should('have.length', 1)
      cy.get('@idErrors').eq(0).should('contain.text', "must have required property 'id'")

      cy.get('[role="alert"]').should('contain.text', "must have required property 'id'")
    })

    it('should render arguments errors', () => {
      cy.mountWithProviders(<BehaviorPolicyPanel selectedNode="3" onFormSubmit={cy.stub} />, { wrapper })

      cy.get('#root_id').type('a123')
      cy_selectBehaviourModel(0)

      cy.get('label[for="root_arguments_minPublishes"] + div > input').clear()
      cy.get('label[for="root_arguments_minPublishes"] + div > input').type('-50')
      cy.get('label[for="root_arguments_maxPublishes"] + div > input').clear()
      cy.get('label[for="root_arguments_maxPublishes"] + div > input').type('-50')
      cy.get("button[type='submit']").click()

      cy.get('[role="group"]:has(> label[for="root_arguments_minPublishes"]) + ul > li').as('minPublishesErrors')
      cy.get('@minPublishesErrors').should('have.length', 1)
      cy.get('@minPublishesErrors').eq(0).should('contain.text', 'must be >= 0')

      cy.get('[role="group"]:has(> label[for="root_arguments_maxPublishes"]) + ul > li').as('maxPublishesErrors')
      cy.get('@maxPublishesErrors').should('have.length', 1)
      cy.get('@maxPublishesErrors').eq(0).should('contain.text', 'must be >= -1')

      cy.get('label[for="root_arguments_minPublishes"] + div > input').clear()
      cy.get('label[for="root_arguments_minPublishes"] + div > input').type('200')
      cy.get('label[for="root_arguments_maxPublishes"] + div > input').clear()
      cy.get('label[for="root_arguments_maxPublishes"] + div > input').type('100')
      cy.get("button[type='submit']").click()

      cy.get('[role="group"]:has(> label[for="root_arguments_minPublishes"]) + ul > li').as('minPublishesErrors')
      cy.get('@minPublishesErrors').should('have.length', 1)
      cy.get('@minPublishesErrors')
        .eq(0)
        .should('contain.text', 'The minimum publish quota must be less than or equal to the maximum publish quota')

      cy.get('[role="group"]:has(> label[for="root_arguments_maxPublishes"]) + ul > li').as('maxPublishesErrors')
      cy.get('@maxPublishesErrors').should('have.length', 1)
      cy.get('@maxPublishesErrors')
        .eq(0)
        .should('contain.text', 'The maximum publish quota must be greater than or equal to the minimum publish quota')
    })
  })
})
