/// <reference types="cypress" />

import LoaderSpinner from './LoaderSpinner.tsx'

describe('LoaderSpinner', () => {
  beforeEach(() => {
    cy.viewport(400, 150)
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<LoaderSpinner />)
    cy.checkAccessibility()
    cy.percySnapshot('Component: Query Loader Spinner')
  })
})
