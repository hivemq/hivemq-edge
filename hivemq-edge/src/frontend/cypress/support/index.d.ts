/// <reference types="cypress" />
import { mount } from 'cypress/react18'

declare namespace Cypress {
  interface Chainable<Subject> {
    mount: typeof mount
  }
}
