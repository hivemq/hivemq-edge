/// <reference types="cypress" />

import { MockStoreWrapper } from '../../__test-utils__/react-flow.mocks.tsx'
import { DataHubNodeType } from '../../types.ts'
import { getNodePayload } from '../../utils/node.utils.ts'
import { ClientFilterPanel } from '../panels/ClientFilterPanel.tsx'

const wrapper: React.JSXElementConstructor<{ children: React.ReactNode }> = ({ children }) => (
  <MockStoreWrapper
    config={{
      initialState: {
        nodes: [
          {
            id: '3',
            type: DataHubNodeType.CLIENT_FILTER,
            position: { x: 0, y: 0 },
            data: getNodePayload(DataHubNodeType.CLIENT_FILTER),
          },
        ],
      },
    }}
  >
    {children}
  </MockStoreWrapper>
)

describe('ClientFilterPanel', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
  })

  it('should render the fields for a Validator', () => {
    const onSubmit = cy.stub().as('onSubmit')

    cy.mountWithProviders(<ClientFilterPanel selectedNode={'3'} onFormSubmit={onSubmit} />, { wrapper })

    cy.get('h5').eq(0).should('contain.text', 'Client Filters')
    // first item
    cy.get('label#root_clients_0-label').should('contain.text', 'clients-0')
    cy.get('label#root_clients_0-label + input').should('have.value', 'client10')
    // first item
    cy.get('label#root_clients_1-label').should('contain.text', 'clients-1')
    cy.get('label#root_clients_1-label + input').should('have.value', 'client20')
    // first item
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
    cy.mountWithProviders(<ClientFilterPanel selectedNode={'3'} />, { wrapper })

    cy.checkAccessibility()
    cy.percySnapshot('Component: ClientFilterPanel')
  })
})
