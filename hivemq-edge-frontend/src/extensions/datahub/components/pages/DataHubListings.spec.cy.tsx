/// <reference types="cypress" />

import DataHubListings from '@datahub/components/pages/DataHubListings.tsx'
import { mockSchemaTempHumidity } from '@datahub/api/hooks/DataHubSchemasService/__handlers__'

describe('DataHubListings', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
    cy.intercept('/api/v1/data-hub/data-validation/policies', { statusCode: 404 })
    cy.intercept('/api/v1/data-hub/behavior-validation/policies', { statusCode: 404 })
    cy.intercept('/api/v1/data-hub/schemas', { statusCode: 404 })
    cy.intercept('/api/v1/data-hub/scripts', { statusCode: 404 })
  })

  it('should render the tabs', () => {
    cy.mountWithProviders(<DataHubListings />)

    cy.getByTestId('list-tabs').find('button[role="tab"]').as('tabs')
    cy.get('@tabs').should('have.length', 3)
    cy.get('@tabs').eq(0).should('have.text', 'Policies')
    cy.get('@tabs').eq(1).should('have.text', 'Schemas')
    cy.get('@tabs').eq(2).should('have.text', 'Scripts')
    cy.get("[role='tabpanel']").should('have.length', 3)
  })

  it('should navigate through the tabs', () => {
    cy.mountWithProviders(<DataHubListings />)

    cy.getByTestId('list-tabs').find('button[role="tab"]').as('tabs')
    cy.get('@tabs').eq(0).click()
    cy.get("[role='tabpanel']").eq(0).should('contain.text', 'This is where all your policies will be listed')

    cy.get('@tabs').eq(1).click()
    cy.get("[role='tabpanel']")
      .eq(1)
      .should('contain.text', 'All your schemas used for validation or transformation will be listed there')

    cy.get('@tabs').eq(2).click()
    cy.get("[role='tabpanel']").eq(2).should('contain.text', 'Your transformation code will be listed there')
  })

  it('should support deletion', () => {
    cy.intercept('/api/v1/data-hub/schemas', { items: [mockSchemaTempHumidity] })
    cy.intercept('/api/v1/data-hub/schemas/my-schema-id', {})
    cy.mountWithProviders(<DataHubListings />)

    cy.getByTestId('list-tabs').find('button[role="tab"]').as('tabs')
    cy.get('@tabs').eq(1).click()
    cy.get('tbody tr').first().as('firstItem')

    cy.get('@firstItem').find('td').as('firstItemContent')
    cy.get('@firstItemContent').should('have.length', 5)
    cy.getByTestId('list-action-delete').click()

    cy.get("[role='alertdialog']").as('modal').should('be.visible')
    cy.get('@modal').find('header').should('have.text', 'Delete Schema')
    cy.get('@modal').find('footer').find('button').as('actions')
    cy.get('@actions').eq(0).click()
    cy.get("[role='alertdialog']").should('not.exist')

    cy.getByTestId('list-action-delete').click()
    cy.get("[role='alertdialog']").should('be.visible')
    cy.get('@actions').eq(1).click()
    cy.get('div#toast-1-title')
      .should('have.attr', 'data-status', 'success')
      .should('be.visible')
      .should('contain.text', 'Schema deleted')
  })
})
