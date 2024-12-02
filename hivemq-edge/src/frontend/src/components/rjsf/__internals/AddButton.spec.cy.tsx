/// <reference types="cypress" />

import { IconButtonProps } from '@rjsf/utils'
import AddButton from '@/components/rjsf/__internals/AddButton.tsx'

const mockIconButtonProps: IconButtonProps = {
  // @ts-ignore
  registry: {
    translateString: (string) => string,
  },
}

describe('AddButton', () => {
  beforeEach(() => {
    cy.viewport(800, 900)
  })

  it('should render properly and be accessible', () => {
    cy.injectAxe()

    cy.mountWithProviders(<AddButton {...mockIconButtonProps} />)
    cy.get('button').should('contain.text', 'Add Item')
    cy.checkAccessibility()
  })

  it('should render a custom title', () => {
    cy.mountWithProviders(<AddButton {...mockIconButtonProps} aria-label="Add a user property" />)
    cy.get('button').should('contain.text', 'Add a user property')
  })
})
