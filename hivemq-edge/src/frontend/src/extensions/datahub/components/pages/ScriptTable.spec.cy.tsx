/// <reference types="cypress" />

import ScriptTable from '@datahub/components/pages/ScriptTable.tsx'

describe('ScriptTable', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
  })

  it('should render the table component', () => {
    cy.mountWithProviders(<ScriptTable />)

    cy.get('table').should('have.attr', 'aria-label', 'List of scripts')
    cy.get('table').find('thead').find('th').should('have.length', 6)
    cy.get('table').find('thead').find('th').eq(0).should('have.text', 'Function name')
    cy.get('table').find('thead').find('th').eq(1).should('have.text', 'Type')
    cy.get('table').find('thead').find('th').eq(2).should('have.text', 'Version')
    cy.get('table').find('thead').find('th').eq(3).should('have.text', 'Description')
    cy.get('table').find('thead').find('th').eq(4).should('have.text', 'Created')
    cy.get('table').find('thead').find('th').eq(5).should('have.text', 'Actions')
  })
})
