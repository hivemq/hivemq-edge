/// <reference types="cypress" />

import ButtonBadge from './ButtonBadge.tsx'
import { FiMail } from 'react-icons/fi'

describe('ButtonBadge', () => {
  beforeEach(() => {
    cy.viewport(400, 150)
  })

  it('should render disabled badge', () => {
    const onClick = cy.stub().as('onClick')
    cy.mountWithProviders(
      <ButtonBadge
        aria-label={'You have no notification'}
        badgeCount={1}
        icon={<FiMail />}
        isDisabled
        onClick={onClick}
      />
    )

    cy.getByAriaLabel('You have no notification').click({ force: true })
    cy.get('@onClick').should('not.have.been.called')
  })

  it('should render properly', () => {
    const onClick = cy.stub().as('onClick')
    cy.mountWithProviders(
      <ButtonBadge aria-label={'You have one notification'} badgeCount={1} icon={<FiMail />} onClick={onClick} />
    )

    cy.getByAriaLabel('You have one notification').click()
    cy.get('@onClick').should('have.been.called')
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<ButtonBadge aria-label={'You have one notification'} badgeCount={1} icon={<FiMail />} />)
    cy.checkAccessibility()
    cy.percySnapshot('Component: ButtonBadge')
  })
})
