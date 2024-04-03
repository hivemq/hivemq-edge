/// <reference types="cypress" />

import { mockDataPolicy } from '../../api/hooks/DataHubDataPoliciesService/__handlers__'
import { mockBehaviorPolicy } from '../../api/hooks/DataHubBehaviorPoliciesService/__handlers__'
import PolicyTable from './PolicyTable.tsx'

describe('PolicyTable', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
    cy.intercept('/api/v1/data-hub/data-validation/policies', { statusCode: 404 })
    cy.intercept('/api/v1/data-hub/behavior-validation/policies', { statusCode: 404 })
  })

  it('should render the table component', () => {
    cy.mountWithProviders(<PolicyTable />)

    cy.get('table').should('have.attr', 'aria-label', 'List of policies')
    cy.get('table').find('thead').find('th').should('have.length', 5)
    cy.get('table').find('thead').find('th').eq(0).should('have.text', 'id')
    cy.get('table').find('thead').find('th').eq(1).should('have.text', 'Type')
    cy.get('table').find('thead').find('th').eq(2).should('have.text', 'Matching')
    cy.get('table').find('thead').find('th').eq(3).should('have.text', 'Created')
    cy.get('table').find('thead').find('th').eq(4).should('have.text', 'Actions')
  })

  it('should render the error message', () => {
    cy.mountWithProviders(<PolicyTable />)

    cy.get('div.chakra-skeleton').should('have.length', 10)
    cy.get('[role="alert"]')
      .should('have.attr', 'data-status', 'error')
      .should('contain.text', 'There was an error loading the data')
  })

  it('should render an empty table', () => {
    cy.mountWithProviders(<PolicyTable />)
    cy.intercept('/api/v1/data-hub/data-validation/policies', [])
    cy.intercept('/api/v1/data-hub/behavior-validation/policies', [])

    cy.get('table').should('have.attr', 'aria-label', 'List of policies')
    cy.get('div.chakra-skeleton').should('have.length', 10)
    cy.get('[role="alert"]').should('have.attr', 'data-status', 'info').should('contain.text', 'No data received yet.')
  })

  it('should render the policies', () => {
    cy.mountWithProviders(<PolicyTable />)
    cy.intercept('/api/v1/data-hub/data-validation/policies', {
      items: [mockDataPolicy],
    })
    cy.intercept('/api/v1/data-hub/behavior-validation/policies', {
      items: [mockBehaviorPolicy],
    })

    cy.get('table').should('have.attr', 'aria-label', 'List of policies')
    cy.get('div.chakra-skeleton').should('have.length', 10)
    cy.get('table').find('tbody').find('tr').should('have.length', 2)
    cy.get('table').find('tbody').find('tr').eq(0).find('td').eq(1).should('have.text', 'Data Policy')
    cy.get('table').find('tbody').find('tr').eq(1).find('td').eq(1).should('have.text', 'Behavior Policy')
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<PolicyTable />)
    cy.intercept('/api/v1/data-hub/data-validation/policies', { statusCode: 404 })
    cy.intercept('/api/v1/data-hub/behavior-validation/policies', { statusCode: 404 })

    cy.checkAccessibility()
    cy.percySnapshot('Component: PolicyTable')
  })
})
