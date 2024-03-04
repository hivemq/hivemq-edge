/// <reference types="cypress" />

import SchemaTable from '@datahub/components/pages/SchemaTable.tsx'
import { mockSchemaTempHumidity } from '@datahub/api/hooks/DataHubSchemasService/__handlers__'

describe('SchemaTable', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
  })

  it('should render the table component', () => {
    cy.mountWithProviders(<SchemaTable />)

    cy.get('table').should('have.attr', 'aria-label', 'List of schemas')
    cy.get('table').find('thead').find('th').should('have.length', 5)
    cy.get('table').find('thead').find('th').eq(0).should('have.text', 'Schema name')
    cy.get('table').find('thead').find('th').eq(1).should('have.text', 'Type')
    cy.get('table').find('thead').find('th').eq(2).should('have.text', 'Version')
    cy.get('table').find('thead').find('th').eq(3).should('have.text', 'Created')
    cy.get('table').find('thead').find('th').eq(4).should('have.text', 'Actions')
  })

  it('should render the data', () => {
    cy.intercept('/api/v1/data-hub/schemas', { items: [mockSchemaTempHumidity] }).as('getSchemas')

    cy.mountWithProviders(<SchemaTable />)
    cy.get('tbody tr').should('have.length', 1)
    cy.get('tbody tr').first().as('firstItem')

    cy.get('@firstItem').find('td').as('firstItemContent')
    cy.get('@firstItemContent').should('have.length', 5)
    cy.get('@firstItemContent').eq(0).should('have.text', 'my-schema-id')
    cy.get('@firstItemContent').eq(1).should('have.text', 'JSON')
    cy.get('@firstItemContent').eq(2).should('have.text', '1')
    cy.get('@firstItemContent').eq(3).should('have.text', '5 months ago')
  })
})
