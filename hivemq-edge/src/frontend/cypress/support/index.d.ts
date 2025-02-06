/// <reference types="cypress" />
import type { mount } from 'cypress/react18'

declare namespace Cypress {
  interface Chainable {
    mount: typeof mount
  }
}

declare module '@cypress/code-coverage/support'
