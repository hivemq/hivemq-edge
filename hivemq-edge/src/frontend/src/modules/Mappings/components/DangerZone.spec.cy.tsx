import DangerZone from './DangerZone'

describe('DangerZone', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
  })

  it('should render properly', () => {
    const onSubmit = cy.stub().as('onSubmit')
    cy.mountWithProviders(<DangerZone onSubmit={onSubmit} />)

    cy.get('button').should('have.text', 'Delete')

    cy.get('@onSubmit').should('not.have.been.called')
    cy.get('[role="alertdialog"]').should('not.exist')
    cy.get('button').click()
    cy.get('[role="alertdialog"]').should('be.visible')
    cy.get('[role="alertdialog"]').within(() => {
      cy.get('header').should('have.text', 'Delete the Combiner?')
      cy.get('header + div').should(
        'have.text',
        'Do you want to delete the combiner and its content? The action cannot be reversed.'
      )

      cy.get('footer').within(() => {
        cy.get('button').eq(0).should('have.text', 'Cancel')

        cy.get('button').eq(0).click()
        cy.get('@onSubmit').should('not.have.been.called')
      })
    })

    cy.get('[role="alertdialog"]').should('not.exist')
    cy.get('button').click()

    cy.get('[role="alertdialog"]').should('be.visible')
    cy.get('[role="alertdialog"]').within(() => {
      cy.get('footer').within(() => {
        cy.get('button').eq(1).should('have.text', 'Delete')

        cy.get('button').eq(1).click()
        cy.get('@onSubmit').should('have.been.called')
      })
    })
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<DangerZone onSubmit={cy.stub} />)
    cy.get('button').click()

    cy.checkAccessibility()
  })
})
