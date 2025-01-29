import DraftStatus from '@datahub/components/helpers/DraftStatus.tsx'

describe('DraftStatus', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
  })

  it('should render', () => {
    cy.mountWithProviders(<DraftStatus />)

    cy.getByTestId('status-container-type').should('contain.text', 'No type')
    cy.getByTestId('status-container-name').should('contain.text', 'Unnamed policy')
    cy.getByTestId('status-container-status').should('contain.text', 'Draft')

    cy.getByTestId('designer-edit-modify').should('have.attr', 'aria-label', 'Modify the policy').should('be.disabled')
    cy.getByTestId('designer-edit-clone')
      .should('have.attr', 'aria-label', 'Clone as a new draft')
      .should('be.disabled')
    cy.getByTestId('designer-edit-clear')
      .should('have.attr', 'aria-label', 'Clear the Designer')
      .should('not.be.disabled')
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<DraftStatus />)
    cy.checkAccessibility()
  })
})
