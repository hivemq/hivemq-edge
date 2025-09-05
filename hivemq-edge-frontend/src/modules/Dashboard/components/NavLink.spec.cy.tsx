/// <reference types="cypress" />

import { WrapperTestRoute } from '@/__test-utils__/hooks/WrapperTestRoute.tsx'
import { NavLink } from '@/modules/Dashboard/components/NavLink.tsx'
import type { MainNavLinkType } from '@/modules/Dashboard/types.ts'
import { IoHomeOutline } from 'react-icons/io5'

const MOCK_LINK: MainNavLinkType = {
  href: '/edge/newLocation',
  label: 'The title',
  icon: <IoHomeOutline />,
}

describe('NavLink', () => {
  beforeEach(() => {
    cy.viewport(350, 800)
  })

  it('should render properly', () => {
    cy.mountWithProviders(<NavLink link={MOCK_LINK} />, { wrapper: WrapperTestRoute })

    cy.getByTestId('test-pathname').should('have.text', '/')
    cy.get('a')
      .should('have.attr', 'href', '/edge/newLocation')
      .should('have.text', 'The title')
      .should('not.be.disabled')
      .should('not.have.attr', 'aria-current')

    cy.get('a').click()
    cy.getByTestId('test-pathname').should('have.text', '/edge/newLocation')
    cy.get('a')
      .should('have.attr', 'href', '/edge/newLocation')
      .should('have.text', 'The title')
      .should('not.be.disabled')
      .should('have.attr', 'aria-current')
  })

  it('should support right addons', () => {
    cy.mountWithProviders(<NavLink link={{ ...MOCK_LINK, rightAddon: <div data-testid="addon">Right</div> }} />)

    cy.get('a').should('contain.text', 'The title')
    cy.get('a').within(() => {
      cy.getByTestId('addon').should('have.text', 'Right')
    })
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<NavLink link={MOCK_LINK} />)
    cy.checkAccessibility()
    cy.get('a').click()
    cy.checkAccessibility(undefined, {
      rules: {
        'color-contrast': { enabled: false },
      },
    })
  })
})
