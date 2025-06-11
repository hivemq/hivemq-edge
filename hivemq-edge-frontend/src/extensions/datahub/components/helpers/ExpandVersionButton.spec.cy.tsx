import { ExpandVersionButton } from '@datahub/components/helpers/ExpandVersionButton.tsx'

describe('ExpandVersionButton', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
  })

  it('should render', () => {
    cy.mountWithProviders(<ExpandVersionButton isExpanded={false} onClick={cy.stub} />)

    cy.getByTestId('list-action-collapse').should('have.attr', 'aria-label', 'Show the versions')
  })

  it('should expand', () => {
    cy.mountWithProviders(<ExpandVersionButton isExpanded={true} onClick={cy.stub} />)

    cy.getByTestId('list-action-collapse').should('have.attr', 'aria-label', 'Hide the versions')
  })

  it('should toggle', () => {
    const onClick = cy.stub().as('onClick')
    cy.mountWithProviders(<ExpandVersionButton isExpanded={false} onClick={onClick} />)

    cy.get('@onClick').should('not.have.been.called')
    cy.getByTestId('list-action-collapse').click()
    cy.get('@onClick').should('have.been.called')
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<ExpandVersionButton isExpanded={true} onClick={cy.stub} />)
    cy.checkAccessibility()
  })
})
