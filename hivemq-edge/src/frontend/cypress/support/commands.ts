/// <reference types="cypress" />
// ***********************************************
// This example commands.ts shows you how to
// create various custom commands and overwrite
// existing commands.
//
// For more comprehensive examples of custom
// commands please read more here:
// https://on.cypress.io/custom-commands
// ***********************************************
//
//
// -- This is a parent command --
// Cypress.Commands.add('login', (email, password) => { ... })
//
//
// -- This is a child command --
// Cypress.Commands.add('drag', { prevSubject: 'element'}, (subject, options) => { ... })
//
//
// -- This is a dual command --
// Cypress.Commands.add('dismiss', { prevSubject: 'optional'}, (subject, options) => { ... })
//
//
// -- This will overwrite an existing command --
// Cypress.Commands.overwrite('visit', (originalFn, url, options) => { ... })
//
import { getByTestId } from './commands/getByTestId.ts'
import { getByAriaLabel } from './commands/getByAriaLabel.ts'
import { checkAccessibility } from './commands/checkAccessibility.ts'
import { clearInterceptList } from './commands/clearInterceptList.ts'

declare global {
  // eslint-disable-next-line @typescript-eslint/no-namespace
  namespace Cypress {
    interface Chainable {
      checkAccessibility: typeof checkAccessibility
      getByTestId: typeof getByTestId
      getByAriaLabel: typeof getByAriaLabel
      clearInterceptList: typeof clearInterceptList
    }
  }
}

Cypress.Commands.add('getByTestId', getByTestId)
Cypress.Commands.add('getByAriaLabel', getByAriaLabel)
Cypress.Commands.add('checkAccessibility', checkAccessibility)
Cypress.Commands.add('clearInterceptList', clearInterceptList)
