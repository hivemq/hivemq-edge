/// <reference types="cypress" />

import { MOCK_NAMESPACE } from '@/__test-utils__/mocks.ts'
import PrefixPanel from './PrefixPanel.tsx'

describe('PrefixPanel', () => {
  beforeEach(() => {
    cy.viewport(800, 600)
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<PrefixPanel data={MOCK_NAMESPACE} />)
    cy.checkAccessibility()
    cy.percySnapshot('Component: PrefixPanel')
  })
})
