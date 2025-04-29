/// <reference types="cypress" />

import { SkipNotification } from './SkipNotification.tsx'

const mockNotificationId = 'mock-id'
describe('SkipNotification', () => {
  beforeEach(() => {
    cy.viewport(300, 500)
  })

  it('should render properly', () => {
    cy.mountWithProviders(<SkipNotification id={mockNotificationId} />)

    cy.getByTestId('notification-skip').should('exist')
    cy.getAllLocalStorage().then((e) => {
      const g = Object.values(e)[0]['edge.notifications']
      cy.wrap(g).should('equal', JSON.stringify([]))
    })

    cy.getByTestId('notification-skip').click()
    cy.getAllLocalStorage().then((e) => {
      const g = Object.values(e)[0]['edge.notifications']
      cy.wrap(g).should('equal', JSON.stringify(['mock-id']))
    })

    cy.getByTestId('notification-skip').click()
    cy.getAllLocalStorage().then((e) => {
      const g = Object.values(e)[0]['edge.notifications']
      cy.wrap(g).should('equal', JSON.stringify([]))
    })
  })
})
