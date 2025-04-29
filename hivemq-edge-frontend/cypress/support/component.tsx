import 'cypress-axe'
import 'cypress-each'
import '@percy/cypress'
import 'cypress-real-events'
import '@cypress/code-coverage/support'

import './commands'

import type { MountOptions, MountReturn } from 'cypress/react'
import { mount } from 'cypress/react'
import type { MemoryRouterProps } from 'react-router-dom'
import { MemoryRouter } from 'react-router-dom'
import { ChakraProvider, VisuallyHidden } from '@chakra-ui/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'

import { AccessibleDraggableProvider } from '@/hooks/useAccessibleDraggable'
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
            <AccessibleDraggableProvider>
              <main style={{ padding: '20px' }}>
                <VisuallyHidden>
                  <h1>Component Testing</h1>
                </VisuallyHidden>
                {Test ? <Test>{component}</Test> : component}
              </main>
            </AccessibleDraggableProvider>
          </MemoryRouter>
        </AuthProvider>
      </ChakraProvider>
    </QueryClientProvider>
  )

  return mount(wrapped, mountOptions)
})
