/// <reference types="cypress" />

import { Button } from '@chakra-ui/react'
import type { Node } from '@xyflow/react'

import { MockStoreWrapper } from '@datahub/__test-utils__/MockStoreWrapper.tsx'
import { mockDataPolicy } from '@datahub/api/hooks/DataHubDataPoliciesService/__handlers__'
import type { TopicFilterData } from '@datahub/types.ts'
import { DataHubNodeType } from '@datahub/types.ts'

import { TopicFilterPanel } from './TopicFilterPanel.tsx'

const MOCK_TOPIC_FILTER: Node<TopicFilterData> = {
  id: '3',
  type: DataHubNodeType.CLIENT_FILTER,
  position: { x: 0, y: 0 },
  data: { topics: ['root/test1', 'root/test2', 'root/test1', 'root/topic/ref/1'] },
}

const wrapper: React.JSXElementConstructor<{ children: React.ReactNode }> = ({ children }) => (
  <MockStoreWrapper
    config={{
      initialState: {
        nodes: [MOCK_TOPIC_FILTER],
      },
    }}
  >
    {children}
    <Button variant="primary" type="submit" form="datahub-node-form">
      SUBMIT{' '}
    </Button>
  </MockStoreWrapper>
)

describe('TopicFilterPanel', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
  })

  it('should render loading and error states', () => {
    cy.intercept('/api/v1/data-hub/data-validation/policies', { statusCode: 404 }).as('getPolicies')
    const onFormError = cy.stub().as('onFormError')
    cy.intercept('/api/v1/data-hub/scripts', { statusCode: 404 }).as('getScripts')
    cy.mountWithProviders(<TopicFilterPanel selectedNode="3" onFormError={onFormError} />, { wrapper })
    cy.getByTestId('loading-spinner').should('be.visible')

    cy.wait('@getPolicies')
    cy.get('[role="alert"]')
      .should('be.visible')
      .should('have.attr', 'data-status', 'error')
      .should('have.text', 'DataPolicy not found')

    cy.get('@onFormError').should('have.been.calledWithErrorMessage', 'DataPolicy not found')
  })

  it('should render the fields for a Validator', () => {
    cy.intercept('/api/v1/data-hub/data-validation/policies', {
      items: [mockDataPolicy],
    }).as('getPolicies')
    cy.mountWithProviders(<TopicFilterPanel selectedNode="3" />, { wrapper })

    cy.get('label#root_adapter-label').should('contain.text', 'Adapter source')

    cy.get('h2').eq(0).should('contain.text', 'Topic Filters')
    cy.get('label#root_topics_0-label').should('contain.text', 'topics-0')
    cy.get('label#root_topics_0-label + input').should('have.value', 'root/test1')
    cy.get('label#root_topics_1-label').should('contain.text', 'topics-1')
    cy.get('label#root_topics_1-label + input').should('have.value', 'root/test2')
  })

  it('should validate properly the topic filters', () => {
    cy.intercept('/api/v1/data-hub/data-validation/policies', {
      items: [mockDataPolicy],
    }).as('getPolicies')
    cy.mountWithProviders(<TopicFilterPanel selectedNode="3" />, { wrapper })
    cy.get('label#root_topics_0-label').should('have.attr', 'data-invalid')
    cy.get('label#root_topics_1-label').should('not.have.attr', 'data-invalid')
    cy.get('label#root_topics_2-label').should('have.attr', 'data-invalid')
    cy.get('label#root_topics_3-label').should('have.attr', 'data-invalid')

    cy.wait('@getPolicies')

    cy.get('button[type=submit]').click()
    cy.get('label#root_topics_3-label').should('have.attr', 'data-invalid')
  })

  it('should be accessible', () => {
    cy.intercept('/api/v1/data-hub/data-validation/policies', {
      items: [mockDataPolicy],
    }).as('getPolicies')
    cy.injectAxe()
    cy.mountWithProviders(<TopicFilterPanel selectedNode="3" />, { wrapper })

    cy.checkAccessibility()
    cy.percySnapshot('Component: TopicFilterPanel')
  })
})
