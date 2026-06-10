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

    shouldContain(text: string) {
      return cy.get('[role="status"]').should('contain', text)
    },
  }

  get toastRegion() {
    return cy.get('[role="region"][aria-label="Notifications-top-right"]')
  }

  /**
   * Dismiss every open toast and wait until the toast portal region holds no focusable content.
   *
   * The Chakra toast manager region (#chakra-toast-manager-top-right) briefly keeps
   * aria-hidden="true" while a closing toast tears down. While that toast's focusable Close button
   * is still mounted inside the hidden region, axe reports an `aria-hidden-focus` violation. Waiting
   * only for the inner [role="status"] to disappear is not enough — the violating node is the region
   * itself — so gate on the region being free of focusable descendants. Call this before an
   * accessibility scan that follows a toast-producing action.
   */
  dismissAllToasts() {
    // Make sure the toast has actually rendered before trying to close it.
    this.toastRegion.find('[role="status"]').should('be.visible')
    this.toastRegion.find('[aria-label="Close"]').click({ multiple: true })
    this.toastRegion.find('[role="status"], button, a[href], [tabindex]:not([tabindex="-1"])').should('not.exist')
  }
}
