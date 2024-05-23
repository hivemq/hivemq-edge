/// <reference types="cypress" />

import { Node } from 'reactflow'
import { Button } from '@chakra-ui/react'

import { MockStoreWrapper } from '@datahub/__test-utils__/MockStoreWrapper.tsx'
import { OperationPanel } from './OperationPanel.tsx'

const getWrapperWith = (initNodes: Node[]) => {
  const Wrapper: React.JSXElementConstructor<{ children: React.ReactNode }> = ({ children }) => {
    return (
      <MockStoreWrapper
        config={{
          initialState: {
            nodes: initNodes,
          },
        }}
      >
        {children}
        <Button variant="primary" type="submit" form="datahub-node-form">
          SUBMIT
        </Button>
      </MockStoreWrapper>
    )
  }

  return Wrapper
}

describe('OperationPanel', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
  })

  it('should render a validating form', () => {
    cy.mountWithProviders(<OperationPanel selectedNode="0" onFormSubmit={cy.stub().as('submit')} />, {
      wrapper: getWrapperWith([]),
    })

    cy.get('label#root_id-label').should('contain.text', 'id')
    cy.get('#root_id').type('a123')

    // first select
    cy.get('label#root_functionId-label').should('contain.text', 'Function')
    cy.get('label#root_functionId-label + div').should('contain.text', '')
    cy.get('label#root_functionId-label + div').click()
    cy.get('label#root_functionId-label + div').find("[role='listbox']").find("[role='option']").as('functionSelect')

    cy.get('@functionSelect').eq(0).should('contain.text', 'System.log')
    cy.get('@functionSelect').eq(7).should('contain.text', 'Mqtt.drop')
    cy.get('@functionSelect').eq(7).click()

    cy.get('@submit').should('not.have.been.calledWith')
    cy.get("button[type='submit']").click()
    cy.get('@submit')
      .should('have.been.calledWith', Cypress.sinon.match.object)
      .its('firstCall.args.0')
      .should('deep.include', {
        edit: true,
        status: 'submitted',
      })
      .its('formData')
      .should('deep.include', { functionId: 'Mqtt.drop', id: 'a123' })
  })

  it('should render validation errors', () => {
    cy.mountWithProviders(<OperationPanel selectedNode="0" onFormSubmit={cy.stub().as('submit')} />, {
      wrapper: getWrapperWith([]),
    })

    cy.get('label#root_functionId-label + div').click()
    cy.get('label#root_functionId-label + div').find("[role='listbox']").find("[role='option']").as('functionSelect')

    cy.get('@functionSelect').eq(1).click()

    cy.get('p#root_formData_applyPolicies__description + label').should('have.text', 'Apply Policies').click()

    cy.get('@submit').should('not.have.been.calledWith')
    cy.get("button[type='submit']").click()
    cy.get('@submit').should('not.have.been.calledWith')

    cy.get('[role="group"]:has(> label#root_id-label) + [role="list"]').as('id_Errors')
    cy.get('@id_Errors').should('contain.text', "must have required property 'id'")

    cy.get('[role="group"]:has(> label#root_formData_topic-label) + [role="list"]').as('topic_Errors')
    cy.get('@topic_Errors').should('contain.text', "must have required property 'Topic'")

    cy.get('[role="alert"][data-status="error"]').should('be.visible')
    cy.get('[role="alert"][data-status="error"] ul li').as('listErrors')
    cy.get('@listErrors').eq(0).should('contain.text', "must have required property 'id'")
    cy.get('@listErrors').eq(1).should('contain.text', "must have required property 'Topic'")
  })
})
