import { Text } from '@chakra-ui/react'
import FormLabel from '@/components/Chakra/FormLabel.tsx'

describe('FormLabel', () => {
  beforeEach(() => {
    cy.viewport(400, 150)
  })

  it('should render the addon properly', () => {
    cy.mountWithProviders(<FormLabel rightAddon={<Text data-testid="add-on">the add-on</Text>}>The label</FormLabel>)

    cy.get('div:has(> label)').within(() => {
      cy.get('label').should('have.text', 'The label')
      cy.getByTestId('add-on').should('have.text', 'the add-on')
    })
  })

  it('should render properly without addon', () => {
    cy.mountWithProviders(<FormLabel>The label</FormLabel>)

    cy.get('div:has(> label)').within(() => {
      cy.get('label').should('have.text', 'The label')
      cy.getByTestId('add-on').should('not.exist')
    })
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<FormLabel rightAddon={<Text data-testid="add-on">the add-on</Text>}>The label</FormLabel>)
    cy.checkAccessibility()
  })
})
