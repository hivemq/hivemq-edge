/// <reference types="cypress" />

import DesignerCheatSheet from '@datahub/components/controls/DesignerCheatSheet.tsx'

describe('DesignerCheatSheet', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
  })

  it('should renders properly ', () => {
    cy.mountWithProviders(<DesignerCheatSheet />)

    cy.get("[role='dialog']").should('not.exist')
    cy.getByTestId('canvas-control-help').click()
    cy.get("[role='dialog']").should('be.visible')
    cy.get("[role='dialog'] header").should('contain.text', 'Keyboard shortcuts')
  })
})
