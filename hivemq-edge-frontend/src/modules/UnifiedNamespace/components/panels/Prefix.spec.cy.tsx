/// <reference types="cypress" />

import PrefixPanel from './PrefixPanel.tsx'
import { MOCK_NAMESPACE } from '@/__test-utils__/mocks.ts'

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
