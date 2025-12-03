import 'cypress-axe'
import 'cypress-each'
import '@percy/cypress'
import 'cypress-real-events'
import '@cypress/code-coverage/support'
import { type MountOptions, type MountReturn, mount } from 'cypress/react'
import type { ReactNode } from 'react'
import type { MemoryRouterProps } from 'react-router-dom'
import { register as registerCypressGrep } from '@cypress/grep'
import installLogsCollector from 'cypress-terminal-report/src/installLogsCollector'

import { mountWithProviders } from './commands/mountWithProviders.tsx'
import './commands'
import './commands/checkI18nKeys'

// Configure logs collector to capture accessibility violations
installLogsCollector({
  collectTypes: ['cy:log', 'cy:xhr', 'cy:request', 'cy:intercept', 'cy:command'],
})

declare global {
  // eslint-disable-next-line @typescript-eslint/no-namespace
  namespace Cypress {
    interface Chainable {
      mount(jsx: ReactNode, options?: MountOptions, rerenderKey?: string): Cypress.Chainable<MountReturn>
      mountWithProviders(
        component: React.ReactNode,
        options?: MountOptions & { routerProps?: MemoryRouterProps } & {
          wrapper?: React.JSXElementConstructor<{ children: React.ReactNode }>
        }
      ): Cypress.Chainable<MountReturn>
    }
  }
}

Cypress.Commands.add('mount', mount)
Cypress.Commands.add('mountWithProviders', mountWithProviders)

registerCypressGrep()
