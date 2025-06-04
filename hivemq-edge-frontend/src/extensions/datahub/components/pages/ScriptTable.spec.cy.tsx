import ScriptTable from '@datahub/components/pages/ScriptTable.tsx'
import { mockScript } from '@datahub/api/hooks/DataHubScriptsService/__handlers__'
import { DateTime } from 'luxon'
import { MOCK_CREATED_AT } from '@/__test-utils__/mocks.ts'

describe('ScriptTable', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
  })

  it('should render the table component', () => {
    cy.intercept('/api/v1/data-hub/scripts', { statusCode: 404 })
    cy.mountWithProviders(<ScriptTable />)

    cy.get('table').should('have.attr', 'aria-label', 'List of scripts')
    cy.get('table').find('thead').find('th').should('have.length', 6)
    cy.get('table').find('thead').find('th').eq(0).should('have.text', 'Function name')
    cy.get('table').find('thead').find('th').eq(1).should('have.text', 'Type')
    cy.get('table').find('thead').find('th').eq(2).should('have.text', 'Version')
    cy.get('table').find('thead').find('th').eq(3).should('have.text', 'Description')
    cy.get('table').find('thead').find('th').eq(4).should('have.text', 'Created')
    cy.get('table').find('thead').find('th').eq(5).should('have.text', 'Actions')

    cy.get("[role='alert']")
      .should('have.attr', 'data-status', 'error')
      .should('contain.text', 'There was an error loading the data')
  })

  it('should render the data', () => {
    cy.intercept('/api/v1/data-hub/scripts', { items: [mockScript] })
    const now = DateTime.fromISO(MOCK_CREATED_AT).plus({ day: 2 }).toJSDate()

    cy.clock(now)
    cy.mountWithProviders(<ScriptTable />)
    cy.get('tbody tr').should('have.length', 1)
    cy.get('tbody tr').first().as('firstItem')

    cy.get('@firstItem').find('td').as('firstItemContent')
    cy.get('@firstItemContent').should('have.length', 6)
    cy.get('@firstItemContent').eq(0).should('have.text', 'my-script-id')
    cy.get('@firstItemContent').eq(1).should('have.text', 'Transformation')
    cy.get('@firstItemContent').eq(2).should('have.text', '1')
    cy.get('@firstItemContent').eq(3).should('have.text', 'this is a description')
    cy.get('@firstItemContent').eq(4).should('have.text', '2 days ago')
  })

  it('should be accessible', () => {
    cy.intercept('/api/v1/data-hub/scripts', { items: [mockScript] })

    cy.injectAxe()
    cy.mountWithProviders(<ScriptTable />)
    cy.checkAccessibility()
  })
})
