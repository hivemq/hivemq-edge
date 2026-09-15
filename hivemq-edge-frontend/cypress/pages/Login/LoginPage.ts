import { Page } from '../Page.ts'

export class LoginPage extends Page {
  /**
   * The login page now gates on GET /api/v1/auth/mode and renders no login mechanism until it
   * resolves (so an OIDC-only deployment never flashes the local form). E2E suites run without a
   * backend on :8080, so that call would fail and the submit button would never appear. Default the
   * mode to local username/password here — the single point every login-dependent spec funnels
   * through. A spec that needs a different mode can intercept /api/v1/auth/mode itself after visit().
   */
  override visit(route?: string) {
    cy.intercept('/api/v1/auth/mode', { modes: ['USERNAME_PASSWORD'] })
    super.visit(route)
  }

  get userLabel() {
    return cy.get('label[for="username"]')
  }

  get passwordLabel() {
    return cy.get('label[for="password"]')
  }

  get userInput() {
    return cy.get('#username')
  }

  get passwordInput() {
    return cy.get('#password')
  }

  get showPassword() {
    return cy.getByAriaLabel('Show password')
  }

  get loginButton() {
    return cy.getByTestId('loginPage-submit')
  }

  get errorMessage() {
    return cy.get('[role="alert"][data-status="error"]')
  }
}

export const loginPage = new LoginPage()
