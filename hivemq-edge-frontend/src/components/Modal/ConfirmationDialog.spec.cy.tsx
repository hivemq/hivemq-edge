import { Button } from '@chakra-ui/react'
import ConfirmationDialog from './ConfirmationDialog'

describe('ConfirmationDialog', () => {
  beforeEach(() => {
    cy.viewport(800, 600)
  })

  it('should render properly', () => {
    const onClose = cy.stub().as('onClose')
    const onSubmit = cy.stub().as('onSubmit')
    cy.mountWithProviders(
      <ConfirmationDialog
        isOpen={true}
        onClose={onClose}
        onSubmit={onSubmit}
        header="Hello"
        message="The nice long message"
        prompt="Are you sure?"
      />
    )

    cy.get('header').should('have.text', 'Hello')
    cy.getByTestId('confirmation-message').should('have.text', 'The nice long message')
    cy.getByTestId('confirmation-prompt').should('have.text', 'Are you sure?')
    cy.getByTestId('confirmation-submit').should('have.text', 'Delete')
    cy.getByTestId('confirmation-cancel').should('have.text', 'Cancel')

    cy.get('@onClose').should('have.not.been.called')
    cy.getByTestId('confirmation-cancel').click()
    cy.get('@onClose').should('have.been.called')

    cy.get('@onSubmit').should('have.not.been.called')
    cy.getByTestId('confirmation-submit').click()
    cy.get('@onSubmit').should('have.been.called')
  })

  it('should render alternative submit', () => {
    cy.mountWithProviders(
      <ConfirmationDialog
        isOpen={true}
        onClose={cy.stub}
        header="Hello"
        message="The nice long message"
        action="Alternative Text"
      />
    )

    cy.get('header').should('have.text', 'Hello')
    cy.getByTestId('confirmation-submit').should('have.text', 'Alternative Text')
  })

  it('should render secondary footer', () => {
    cy.mountWithProviders(
      <ConfirmationDialog
        isOpen={true}
        onClose={cy.stub}
        header="Hello"
        message="The nice long message"
        footer={<Button>A new action</Button>}
      />
    )

    cy.get('header').should('have.text', 'Hello')
    cy.get('footer').within(() => {
      cy.get('button').eq(0).should('have.text', 'Cancel')
      cy.get('button').eq(1).should('have.text', 'A new action')
      cy.get('button').eq(2).should('have.text', 'Delete')
    })
  })

  it('should be accessible ', () => {
    cy.injectAxe()
    cy.mountWithProviders(
      <ConfirmationDialog isOpen={true} onClose={cy.stub} header="Hello" message="sssss" prompt="sss" />
    )
    cy.get('header').should('be.visible')

    cy.checkAccessibility()
  })
})
