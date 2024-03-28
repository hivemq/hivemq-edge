import { Editor } from '@datahub/components/interpolation/Editor.tsx'

describe('Editor', () => {
  beforeEach(() => {
    cy.viewport(400, 600)
  })

  it('should render the editor', () => {
    cy.mountWithProviders(<Editor id="my-id" labelId="my-label-id" value="This is a test" />)
    cy.get('#my-id').should('contain.text', 'This is a test')
    cy.get('#my-id').click()
    cy.get('#my-id').type('{selectall}')
    cy.get('#my-id').type('A new topic @')
    cy.getByTestId('interpolation-container').should('be.visible')
    // cy.getByTestId('interpolation-container').type('{downArrow}')
    cy.getByTestId('interpolation-container').find('button').eq(4).click()
    cy.get('#my-id').should('contain.text', 'A new topic @validationResult')
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<Editor id="my-id" labelId="my-label-id" value="This is a test ${validationResult}" />)
    cy.checkAccessibility()
  })
})
