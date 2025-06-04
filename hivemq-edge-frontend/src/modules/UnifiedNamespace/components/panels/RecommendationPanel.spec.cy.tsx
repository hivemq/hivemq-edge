/// <reference types="cypress" />

import RecommendationPanel from '@/modules/UnifiedNamespace/components/panels/RecommendationPanel.tsx'

describe('RecommendationPanel', () => {
  beforeEach(() => {
    cy.viewport(800, 600)
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<RecommendationPanel />)
    cy.checkAccessibility()
    cy.percySnapshot('Component: RecommendationPanel')
  })
})
