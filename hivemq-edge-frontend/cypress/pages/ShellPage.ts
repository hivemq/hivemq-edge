import { Page } from './Page.ts'

export class ShellPage extends Page {
  get navLinks() {
    return cy.get('nav [role="list"]')
  }

  get toasts() {
    return cy.get('[role="region"][aria-label="Notifications-top-right"] [role="status"] [data-status]')
  }

  get location() {
    return cy.location('pathname')
  }

  get closeToast() {
    return cy.get('[role="region"][aria-label="Notifications-top-right"] [role="status"] [data-status] button')
  }

  toast = {
    get success() {
      return cy.get('[role="region"][aria-label="Notifications-top-right"] [role="status"] [data-status="success"]')
    },

    get error() {
      return cy.get('[role="region"][aria-label="Notifications-top-right"] [role="status"] > [data-status="error"]')
    },

    close() {
      cy.get('[role="status"]').within(() => {
        cy.getByAriaLabel('Close').click()
      })
    },
  }
}
