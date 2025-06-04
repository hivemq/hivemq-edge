import ObservabilityEdgeCTA from './ObservabilityEdgeCTA.tsx'

describe('ObservabilityEdgeCTA', () => {
  beforeEach(() => {
    cy.viewport(400, 400)
  })

  it('should render the button', () => {
    cy.mountWithProviders(<ObservabilityEdgeCTA source="1" onClick={cy.stub().as('onClick')} />)
    cy.get('button').eq(0).should('not.have.attr', 'aria-describedby')
    cy.get('button').eq(0).focus()
    cy.get('button').eq(0).should('have.attr', 'aria-describedby', 'tooltip-:r1:')
    cy.getByTestId(`icon-button-tooltip`).eq(0).should('contain.text', 'Open the Observability panel')
    cy.get('@onClick').should('not.have.been.called')
    cy.get('button').eq(0).click()
    cy.get('@onClick').should('have.been.called')
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<ObservabilityEdgeCTA source="1" />)
    cy.checkAccessibility()
  })
})
