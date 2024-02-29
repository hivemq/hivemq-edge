/// <reference types="cypress" />

import DataHubListAction from '@datahub/components/helpers/DataHubListAction.tsx'

describe('DataHubListAction', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
  })

  it('should render the actions', () => {
    cy.mountWithProviders(<DataHubListAction onEdit={cy.stub().as('onEdit')} onDelete={cy.stub().as('onDelete')} />)

    cy.get('button').should('have.length', 2)
    cy.getByTestId('list-action-edit').should('not.be.disabled')
    cy.getByTestId('list-action-edit').click()
    cy.get('@onEdit').should('have.been.called')
    cy.getByTestId('list-action-delete').click()
    cy.get('@onDelete').should('have.been.called')
  })

  it('should render the buttons disabled', () => {
    cy.mountWithProviders(<DataHubListAction isEditDisabled />)

    cy.get('button').should('have.length', 2)
    cy.getByTestId('list-action-edit').should('be.disabled')
  })
})
