/// <reference types="cypress" />

import { Button } from '@chakra-ui/react'

import { MockStoreWrapper } from '@datahub/__test-utils__/MockStoreWrapper.tsx'
import { mockDataPolicy } from '@datahub/api/hooks/DataHubDataPoliciesService/__handlers__'
import { DataPolicyPanel } from '@datahub/designer/data_policy/DataPolicyPanel.tsx'
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

describe('DataPolicyPanel', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
    cy.intercept('/api/v1/data-hub/data-validation/policies', {
      items: [mockDataPolicy],
    })
  })

  it('should render the fields for the panel', () => {
    const onSubmit = cy.stub().as('onSubmit')

    // eslint-disable-next-line react/jsx-no-undef
    cy.mountWithProviders(<DataPolicyPanel selectedNode="3" onFormSubmit={onSubmit} />, { wrapper })

    cy.get('label#root_id-label').should('contain.text', 'id')
    cy.get('#root_id').type('123')

    cy.get("button[type='submit']").click()
    cy.get('@onSubmit')
      .should('have.been.calledOnceWith', Cypress.sinon.match.object)
      .its('firstCall.args.0')
      .should('deep.include', {
        status: 'submitted',
        formData: {
          id: '123',
        },
      })
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<DataPolicyPanel selectedNode="3" />, { wrapper })

    cy.checkAccessibility()
    cy.percySnapshot('Component: DataPolicyPanel')
  })
})
