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

  get loginButton() {
    return cy.getByTestId('loginPage-submit')
  }
}

export const loginPage = new LoginPage()
