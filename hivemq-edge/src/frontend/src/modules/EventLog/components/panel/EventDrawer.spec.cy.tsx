/// <reference types="cypress" />

import { mockEdgeEvent } from '@/api/hooks/useEvents/__handlers__'
import EventDrawer from './EventDrawer.tsx'

describe('EventDrawer', () => {
  beforeEach(() => {
    cy.viewport(1200, 800)
    cy.window().then((win) => {
      Object.defineProperty(win.navigator, 'language', { value: 'en-GB' })
    })
  })

  it('should render the side panel', () => {
    cy.mountWithProviders(<EventDrawer isOpen={true} onClose={cy.stub()} event={mockEdgeEvent(1)[0]} />)
    cy.get('#bridge-form-header').should('contain.text', 'Event')

    cy.get('[data-status]').should('contain.text', 'INFO')
    cy.getByTestId('event-title-created').should('contain.text', 'Created')
    cy.getByTestId('event-value-created').should('contain.text', 'Friday, 13 October 2023 at 11:51:24.234')
    cy.getByTestId('event-title-source').should('contain.text', 'Source')
    cy.getByTestId('event-value-source').should('contain.text', 'BRIDGE-0')
    cy.getByTestId('event-title-associatedObject').should('contain.text', 'Associated Object')
    cy.getByTestId('event-value-associatedObject').should('contain.text', 'BRIDGE-0')

    cy.getByTestId('event-value-message').should('contain.text', 'Lorem ipsum dolor sit amet')
    cy.getByTestId('event-title-payload').should('contain.text', 'Payload')
    cy.getByTestId('event-title-payload').should('contain.text', 'XML')
  })

  it('should open and close the side panel', () => {
    const mockOnClose = cy.stub().as('onClose')

    cy.mountWithProviders(<EventDrawer isOpen={true} onClose={mockOnClose} event={mockEdgeEvent(1)[0]} />)
    cy.get('#bridge-form-header').should('contain.text', 'Event')
    cy.getByAriaLabel('Close').click()
    cy.get('@onClose').should('be.called')
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<EventDrawer isOpen={true} onClose={cy.stub()} event={mockEdgeEvent(1)[0]} />)
    cy.checkAccessibility(undefined, {
      rules: {
        // TODO[#17144] Color-contrast fixed but still not passing
        'color-contrast': { enabled: false },
      },
    })
    cy.percySnapshot('Component: EventDrawer')
  })
})
