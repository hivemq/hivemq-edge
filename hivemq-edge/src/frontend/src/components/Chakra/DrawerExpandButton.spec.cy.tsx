import DrawerExpandButton from '@/components/Chakra/DrawerExpandButton.tsx'

describe('DrawerExpandButton', () => {
  beforeEach(() => {
    cy.viewport(400, 150)
  })

  it('should render expanded properly', () => {
    // eslint-disable-next-line react/jsx-no-undef
    cy.mountWithProviders(<DrawerExpandButton isExpanded={true} toggle={cy.stub().as('toggle')} />)
    cy.get('button').should('have.attr', 'data-isExpanded', 'true')

    cy.get('@toggle').should('not.have.been.called')
    cy.get('button').click()
    cy.get('@toggle').should('have.been.called')
  })

  it('should render shrunk properly', () => {
    // eslint-disable-next-line react/jsx-no-undef
    cy.mountWithProviders(<DrawerExpandButton isExpanded={false} toggle={cy.stub().as('toggle')} />)
    cy.get('button').should('have.attr', 'data-isExpanded', 'false')

    cy.get('@toggle').should('not.have.been.called')
    cy.get('button').click()
    cy.get('@toggle').should('have.been.called')
  })
})
