/// <reference types="cypress" />

import EventDrawer from './EventDrawer.tsx'

describe('EventDrawer', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
  })

  it('should open and close the side panel', () => {
    const mockOnClose = cy.stub().as('onClose')

    cy.mountWithProviders(<EventDrawer isOpen={true} onClose={mockOnClose} event={undefined} />)
    cy.get('#bridge-form-header').should('contain.text', 'Event')
    cy.getByAriaLabel('Close').click()
    cy.get('@onClose').should('be.called')
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<EventDrawer isOpen={true} onClose={cy.stub()} event={undefined} />)
    cy.checkAccessibility()
    cy.percySnapshot('Component: AdapterActionMenu')
  })
})
