/// <reference types="cypress" />

import DataHubListAction from '@datahub/components/helpers/DataHubListAction.tsx'

describe('DataHubListAction', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
  })

  it('should render the actions', () => {
    cy.mountWithProviders(<DataHubListAction onEdit={cy.stub().as('onEdit')} onDelete={cy.stub().as('onDelete')} />)

    cy.get('button').should('have.length', 3)
    cy.getByTestId('list-action-view').should('not.be.disabled')

    cy.get('@onEdit').should('not.have.been.called')
    cy.getByTestId('list-action-view').click()
    cy.get('@onEdit').should('have.been.called')

    cy.get('@onDelete').should('not.have.been.called')
    cy.getByTestId('list-action-delete').click()
    cy.get('@onDelete').should('have.been.called')
  })

  it('should render the buttons disabled', () => {
    cy.mountWithProviders(<DataHubListAction isAccessDisabled />)

    cy.get('button').should('have.length', 2)
    cy.getByTestId('list-action-view').should('not.exist')
  })
})
