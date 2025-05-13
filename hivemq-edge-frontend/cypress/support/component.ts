// ***********************************************************
// This example support/component.ts is processed and
// loaded automatically before your test files.
//
// This is a great place to put global configuration and
// behavior that modifies Cypress.
//
// You can change the location of this file or turn off
// automatically serving support files with the
// 'supportFile' configuration option.
//
// You can read more here:
// https://on.cypress.io/configuration
// ***********************************************************

import 'cypress-axe'
import 'cypress-each'
import '@percy/cypress'
import 'cypress-real-events'
import '@cypress/code-coverage/support'

// Import commands.js using ES2015 syntax:
import './commands'
import { mount } from 'cypress/react'
import { mountWithProviders } from './commands/mountWithProviders.tsx'

Cypress.Commands.add('mount', mount)
Cypress.Commands.add('mountWithProviders', mountWithProviders)
