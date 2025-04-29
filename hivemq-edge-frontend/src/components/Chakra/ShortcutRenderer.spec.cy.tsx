/// <reference types="cypress" />

import ShortcutRenderer from '@/components/Chakra/ShortcutRenderer.tsx'

describe('ShortcutRenderer', () => {
  beforeEach(() => {
    cy.viewport(400, 150)
  })

  it('should render a term and its definition', () => {
    cy.mountWithProviders(<ShortcutRenderer hotkeys="CTRL+C" description="This is a description" />)

    cy.get('[role="term"]').should('contain.text', 'CTRL + C')
    cy.get('kbd').should('have.length', 2)
    cy.get('kbd').eq(0).should('contain.text', 'CTRL')
    cy.get('kbd').eq(1).should('contain.text', 'C')
    cy.get('[role="definition"]').should('contain.text', 'This is a description')
  })

  it('should render multiple shortcuts', () => {
    cy.mountWithProviders(<ShortcutRenderer hotkeys="CTRL+C,Meta+V,ESC" description="This is a description" />)
    const cmd = Cypress.platform === 'darwin' ? '⌘' : '^'

    cy.get('[role="term"]').should('contain.text', `CTRL + C , ${cmd} + V , ESC`)
    cy.get('kbd').should('have.length', 5)
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<ShortcutRenderer hotkeys="CTRL+C" description="This is a description" />)
    cy.checkAccessibility()
    cy.percySnapshot('Component: ShortcutRenderer')
  })
})
