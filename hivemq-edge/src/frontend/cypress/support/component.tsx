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

// TODO[NVL] See https://github.com/cypress-io/cypress/issues/21434
import './workaround-cypress-10-0-2-process-issue.ts'

import 'cypress-axe'
import 'cypress-each'
import '@percy/cypress'
import 'cypress-real-events'
import '@cypress/code-coverage/support'

import './commands'

import type { MountOptions, MountReturn } from 'cypress/react18'
import { mount } from 'cypress/react18'
import type { MemoryRouterProps } from 'react-router-dom'
import { MemoryRouter } from 'react-router-dom'
import { ChakraProvider, VisuallyHidden } from '@chakra-ui/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'

import { AuthProvider } from '@/modules/Auth/AuthProvider.tsx'
import themeHiveMQ from '@/modules/Theme/themeHiveMQ.ts'
import '@/config/i18n.config.ts'
import '@fontsource/roboto/400.css'
import '@fontsource/roboto/700.css'

// Augment the Cypress namespace to include type definitions for
// your custom command.
// Alternatively, can be defined in cypress/support/component.d.ts
// with a <reference path="./component" /> at the top of your spec.
declare global {
  // eslint-disable-next-line @typescript-eslint/no-namespace
  namespace Cypress {
    interface Chainable {
      mount: typeof mount

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
Cypress.Commands.add('mountWithProviders', (component, options = {}) => {
  const { routerProps = { initialEntries: ['/'] }, wrapper: Test, ...mountOptions } = options

  const wrapped = (
    <QueryClientProvider
      client={
        new QueryClient({
          defaultOptions: {
            queries: { retry: 1 },
          },
        })
      }
    >
      <ChakraProvider theme={themeHiveMQ}>
        <AuthProvider>
          <MemoryRouter {...routerProps}>
            <main style={{ padding: '20px' }}>
              <VisuallyHidden>
                <h1>Component Testing</h1>
              </VisuallyHidden>
              {Test ? <Test>{component}</Test> : component}
            </main>
          </MemoryRouter>
        </AuthProvider>
      </ChakraProvider>
    </QueryClientProvider>
  )

  return mount(wrapped, mountOptions)
})
