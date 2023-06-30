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

// Import commands.js using ES2015 syntax:
import './commands'

// Alternatively you can use CommonJS syntax:
// require('./commands')

import { mount, MountOptions, MountReturn } from 'cypress/react18'
import { MemoryRouterProps, MemoryRouter } from 'react-router-dom'
import { AuthProvider } from '../../src/modules/Auth/AuthProvider.tsx'
import { ChakraProvider } from '@chakra-ui/react'
import { themeHiveMQ } from '../../src/modules/App/themes/themeHiveMQ.ts'
import queryClient from '../../src/api/queryClient.ts'
import { QueryClientProvider } from '@tanstack/react-query'

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
        options?: MountOptions & { routerProps?: MemoryRouterProps }
      ): Cypress.Chainable<MountReturn>
    }
  }
}

Cypress.Commands.add('mount', mount)
Cypress.Commands.add('mountWithProviders', (component, options = {}) => {
  const { routerProps = { initialEntries: ['/'] }, ...mountOptions } = options

  const wrapped = (
    <QueryClientProvider client={queryClient}>
      <ChakraProvider theme={themeHiveMQ}>
        <AuthProvider>
          <MemoryRouter {...routerProps}>{component}</MemoryRouter>
        </AuthProvider>
      </ChakraProvider>
    </QueryClientProvider>
  )

  return mount(wrapped, mountOptions)
})
