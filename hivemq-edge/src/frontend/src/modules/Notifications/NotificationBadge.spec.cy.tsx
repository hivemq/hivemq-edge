/// <reference types="cypress" />

import NotificationBadge from '@/modules/Notifications/NotificationBadge.tsx'
import { MOCK_NOTIFICATIONS } from '@/api/hooks/useFrontendServices/__handlers__'

describe('NotificationBadge', () => {
  beforeEach(() => {
    cy.viewport(900, 500)
    cy.intercept('https://api.github.com/repos/hivemq/hivemq-edge/releases', []).as('getReleases')
    cy.intercept('/api/v1/frontend/notifications', { items: MOCK_NOTIFICATIONS }).as('getNotifications')
  })

  it('should render properly', () => {
    cy.mountWithProviders(<NotificationBadge />)

    cy.wait('@getReleases')
    cy.wait('@getNotifications')

    cy.getByTestId('buttonBadge-counter').should('contain.text', 1)
    cy.getByAriaLabel('Notifications').click()

    cy.get("[role='status']").should('be.visible').should('contain.text', 'Default Credentials Need Changing!')
  })
})
