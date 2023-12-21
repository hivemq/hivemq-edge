/// <reference types="cypress" />

import IconButton from './IconButton.tsx'
import { MdLightMode } from 'react-icons/md'

describe('IconButton', () => {
  beforeEach(() => {
    cy.viewport(400, 150)
  })

  it('should render properly', () => {
    cy.mountWithProviders(<IconButton id={'toolTipButton'} aria-label={'This is the button'} icon={<MdLightMode />} />)

    cy.get('#toolTipButton').click()
    cy.get("[role='tooltip']").should('contain.text', 'This is the button')
  })
})
