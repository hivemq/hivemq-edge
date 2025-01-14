import DesignerCheatSheet from '@datahub/components/controls/DesignerCheatSheet.tsx'

describe('DesignerCheatSheet', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
  })

  it('should render properly ', () => {
    cy.mountWithProviders(<DesignerCheatSheet />)

    cy.get("[role='dialog']").should('not.exist')
    cy.getByTestId('canvas-control-help').click()
    cy.get("[role='dialog']").should('be.visible')
    cy.get("[role='dialog'] header").should('contain.text', 'Keyboard shortcuts')
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<DesignerCheatSheet />)
    cy.getByTestId('canvas-control-help').click()

    cy.checkAccessibility()
    cy.percySnapshot('Component: DesignerCheatSheet')
  })
})
