/// <reference types="cypress" />

import { Button } from '@chakra-ui/react'

import { MockStoreWrapper } from '@datahub/__test-utils__/MockStoreWrapper.tsx'
import { DataHubNodeType } from '@datahub/types.ts'
import { getNodePayload } from '@datahub/utils/node.utils.ts'
import { ValidatorPanel } from '@datahub/designer/validator/ValidatorPanel.tsx'

const wrapper: React.JSXElementConstructor<{ children: React.ReactNode }> = ({ children }) => (
  <MockStoreWrapper
    config={{
      initialState: {
        nodes: [
          {
            id: '3',
            type: DataHubNodeType.VALIDATOR,
            position: { x: 0, y: 0 },
            data: getNodePayload(DataHubNodeType.VALIDATOR),
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

describe('ValidatorPanel', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
  })

  it('should render loading and error states', () => {
    const onFormError = cy.stub().as('onFormError')
    cy.mountWithProviders(<ValidatorPanel selectedNode="fakenosde" onFormError={onFormError} />, { wrapper })

    cy.get('[role="alert"]')
      .should('be.visible')
      .should('have.attr', 'data-status', 'error')
      .should('contain.text', 'The Policy Validator is not a valid element')

    cy.get('@onFormError').should('have.been.calledWithErrorMessage', 'The Policy Validator is not a valid element')
  })

  it('should render the fields for a Validator', () => {
    cy.mountWithProviders(<ValidatorPanel selectedNode="3" />, { wrapper })

    // first select
    cy.get('label#root_type-label').should('contain.text', 'Validator Type')
    cy.get('label#root_type-label + div').should('contain.text', 'SCHEMA')
    cy.get('label#root_type-label + div').click()
    cy.get('label#root_type-label + div')
      .find("[role='listbox']")
      .find("[role='option']")
      .eq(0)
      .should('contain.text', 'SCHEMA')
    cy.get('label#root_type-label + div').click()

    // second select
    cy.get('label#root_strategy-label').should('contain.text', 'Validation Strategy')
    cy.get('label#root_strategy-label + div').should('contain.text', 'ALL_OF')
    cy.get('label#root_strategy-label + div').click()
    cy.get('label#root_strategy-label + div')
      .find("[role='listbox']")
      .find("[role='option']")
      .eq(0)
      .should('contain.text', 'ANY_OF')
    cy.get('label#root_strategy-label + div').click()
  })

  it('should render the error message with data not found', () => {
    cy.mountWithProviders(<ValidatorPanel selectedNode="not valid" />, { wrapper })

    cy.get("[role='alert']").find('div[data-status]').should('have.length', 2)
    cy.get("[role='alert']").find('div[data-status]').eq(0).should('contain.text', 'Error loading the node')
    cy.get("[role='alert']")
      .find('div[data-status]')
      .eq(1)
      .should('contain.text', 'The Policy Validator is not a valid element')
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<ValidatorPanel selectedNode="3" />, { wrapper })

    cy.checkAccessibility()
    cy.percySnapshot('Component: ValidatorPanel')
  })
})
