import { DateTime } from 'luxon'
import { MOCK_CREATED_AT } from '@/__test-utils__/mocks.ts'

import { mockSchemaTempHumidity } from '@datahub/api/hooks/DataHubSchemasService/__handlers__'
import SchemaTable from '@datahub/components/pages/SchemaTable.tsx'

describe('SchemaTable', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
    cy.intercept('/api/v1/data-hub/schemas', { statusCode: 404 })

    // TODO[NVL] Create a custom command for that stub
    cy.stub(DateTime, 'now').returns(DateTime.fromISO(MOCK_CREATED_AT).plus({ day: 2 }))
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
    cy.intercept('/api/v1/data-hub/schemas', { items: [mockSchemaTempHumidity] })

    cy.mountWithProviders(<SchemaTable />)
    cy.get('tbody tr').should('have.length', 1)
    cy.get('tbody tr').first().as('firstItem')

    cy.get('@firstItem').find('td').as('firstItemContent')
    cy.get('@firstItemContent').should('have.length', 5)
    cy.get('@firstItemContent').eq(0).should('have.text', 'my-schema-id')
    cy.get('@firstItemContent').eq(1).should('have.text', 'JSON')
    cy.get('@firstItemContent').eq(2).should('have.text', '1')
    cy.get('@firstItemContent').eq(3).should('have.text', '2 days ago')
  })

  it('should render expandable data', () => {
    cy.intercept('/api/v1/data-hub/schemas', {
      items: [mockSchemaTempHumidity, { ...mockSchemaTempHumidity, version: 2 }],
    }).as('getSchemas')

    cy.mountWithProviders(<SchemaTable />)
    cy.wait('@getSchemas')

    cy.get('tbody tr').should('have.length', 1)
    cy.get('tbody tr').first().as('firstItem')

    cy.get('@firstItem').find('td').as('firstItemContent')
    cy.get('@firstItemContent').should('have.length', 5)
    cy.get('@firstItemContent').eq(0).should('have.text', 'my-schema-id')
    cy.get('@firstItemContent').eq(1).should('have.text', 'JSON')
    cy.get('@firstItemContent').eq(2).should('have.text', '2 versions')
    cy.get('@firstItemContent').eq(3).should('have.text', '2 days ago')
    cy.get('@firstItemContent')
      .eq(0)
      .within(() => {
        cy.getByAriaLabel('Show the versions').should('be.visible').should('not.be.disabled')
        cy.getByAriaLabel('Show the versions').click()
      })
    cy.get('tbody tr').should('have.length', 3)
    cy.get('tbody tr').eq(2).find('td').as('childrenRow')
    cy.get('@childrenRow').eq(2).should('have.text', '2')

    cy.get('@firstItemContent')
      .eq(0)
      .within(() => {
        cy.getByAriaLabel('Hide the versions').should('be.visible').should('not.be.disabled')
        cy.getByAriaLabel('Hide the versions').click()
      })
    cy.get('tbody tr').should('have.length', 1)
  })

  it('should be accessible', () => {
    cy.intercept('/api/v1/data-hub/schemas', {
      items: [mockSchemaTempHumidity, { ...mockSchemaTempHumidity, version: 2 }],
    }).as('getSchemas')

    cy.injectAxe()
    cy.mountWithProviders(<SchemaTable />)

    cy.checkAccessibility()
  })
})
