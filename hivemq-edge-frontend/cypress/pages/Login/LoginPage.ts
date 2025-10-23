import { Page } from '../Page.ts'

export class LoginPage extends Page {
  get userLabel() {
    return cy.get('#field-\\:r1\\:-label')
  }

  get passwordLabel() {
    return cy.get('#field-\\:r3\\:-label')
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
