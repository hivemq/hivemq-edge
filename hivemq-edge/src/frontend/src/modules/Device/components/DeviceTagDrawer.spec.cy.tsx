/// <reference types="cypress" />

import DeviceTagDrawer from '@/modules/Device/components/DeviceTagDrawer.tsx'

describe('DeviceTagDrawer', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
  })

  it('should render in disabled state', () => {
    cy.mountWithProviders(<DeviceTagDrawer context={{}} isDisabled />, {
      routerProps: { initialEntries: [`/node/wrong-adapter`] },
    })
    cy.getByAriaLabel('Edit tags').should('be.visible').should('be.disabled')
    cy.get('[role="dialog"]').should('not.exist')
  })

  it('should properly', () => {
    const onSubmit = cy.stub().as('onSubmit')
    cy.mountWithProviders(<DeviceTagDrawer context={{}} onSubmit={onSubmit} />, {
      routerProps: { initialEntries: [`/node/wrong-adapter`] },
    })

    cy.getByAriaLabel('Edit tags').should('be.visible').should('not.be.disabled')
    cy.getByAriaLabel('Edit tags').click()

    cy.get('[role="dialog"]').should('be.visible')
    cy.get('header').should('contain.text', 'Edit the tags')

    cy.get('button[type="submit"]').should('contain.text', 'Submit')
    cy.injectAxe()
    cy.checkAccessibility()
  })
})
