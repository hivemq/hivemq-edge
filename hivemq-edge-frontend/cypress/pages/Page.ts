export abstract class Page {
  visit(route?: string) {
    cy.viewport(1400, 1016)
    // Cypress starts out with a blank slate for each test
    // so we must tell it to visit our website with the `cy.visit()` command.
    // Since we want to visit the same URL at the start of all our tests,
    // we include it in our beforeEach function so that it runs before each test
    cy.visit(route || '/app/', {
      onBeforeLoad(win: Cypress.AUTWindow) {
        // This config need to be more controllable
        win.localStorage.setItem('edge.privacy', JSON.stringify({ heapAnalytics: false, sentry: false }))
      },
    })
  }

  get pageHeader() {
    return cy.get('main header > h1')
  }

  get pageHeaderSubTitle() {
    return cy.get('main header > p')
  }

  toast = {
    close() {
      cy.get('[role="status"]').within(() => {
        cy.getByAriaLabel('Close').click()
      })
    },
  }
}
