/// <reference types="cypress" />

import { Button } from '@chakra-ui/react'
import type { Node } from '@xyflow/react'

import { MockStoreWrapper } from '@datahub/__test-utils__/MockStoreWrapper.tsx'
import type { ClientFilterData } from '@datahub/types.ts'
import { DataHubNodeType } from '@datahub/types.ts'

import { ClientFilterPanel } from './ClientFilterPanel.tsx'

const MOCK_CLIENT_FILTER: Node<ClientFilterData> = {
  id: '3',
  type: DataHubNodeType.CLIENT_FILTER,
  position: { x: 0, y: 0 },
  data: { clients: ['client10', 'client20', 'client30'] },
}

const wrapper: React.JSXElementConstructor<{ children: React.ReactNode }> = ({ children }) => (
  <MockStoreWrapper
    config={{
      initialState: {
        nodes: [MOCK_CLIENT_FILTER],
      },
    }}
  >
    {children}
    <Button variant="primary" type="submit" form="datahub-node-form">
      SUBMIT{' '}
    </Button>
  </MockStoreWrapper>
)

describe('ClientFilterPanel', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
  })

  it('should render the fields for a Validator', () => {
    const onSubmit = cy.stub().as('onSubmit')

    cy.mountWithProviders(<ClientFilterPanel selectedNode="3" onFormSubmit={onSubmit} />, { wrapper })

    cy.get('h2').eq(0).should('contain.text', 'Client Filters')
    cy.get('label#root_clients_0-label').should('contain.text', 'clients-0')
    cy.get('label#root_clients_0-label + input').should('have.value', 'client10')
    cy.get('label#root_clients_1-label').should('contain.text', 'clients-1')
    cy.get('label#root_clients_1-label + input').should('have.value', 'client20')
    cy.get('label#root_clients_2-label').should('contain.text', 'clients-2')
    cy.get('label#root_clients_2-label + input').should('have.value', 'client30')

    cy.get("button[type='submit']").click()
    cy.get('@onSubmit')
      .should('have.been.calledOnceWith', Cypress.sinon.match.object)
      .its('firstCall.args.0')
      .should('deep.include', {
        status: 'submitted',
        formData: { clients: ['client10', 'client20', 'client30'] },
      })
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<ClientFilterPanel selectedNode="3" />, { wrapper })

    cy.checkAccessibility()
    cy.percySnapshot('Component: ClientFilterPanel')
  })
})
