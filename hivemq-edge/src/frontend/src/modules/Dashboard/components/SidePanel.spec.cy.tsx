/// <reference types="cypress" />

import SidePanel from '@/modules/Dashboard/components/SidePanel.tsx'

describe('SidePanel', () => {
  beforeEach(() => {
    cy.viewport(350, 800)
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<SidePanel />)
    cy.checkAccessibility()
  })
})
