import 'cypress-axe'
import 'cypress-each'
import '@percy/cypress'
import 'cypress-real-events'
import '@cypress/code-coverage/support'

import './commands'
import { mount } from 'cypress/react'
import { mountWithProviders } from './commands/mountWithProviders.tsx'

Cypress.Commands.add('mount', mount)
Cypress.Commands.add('mountWithProviders', mountWithProviders)
