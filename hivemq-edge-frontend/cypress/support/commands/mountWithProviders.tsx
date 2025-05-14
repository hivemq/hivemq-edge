import type { MountOptions } from 'cypress/react'
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

/**
 * * Attempts to find an element with a test id attribute of the given param value
 * @example
 * cy.getByTestId('btn')
 * */
export const mountWithProviders = (
  component: React.ReactNode,
  options?: MountOptions & { routerProps?: MemoryRouterProps } & {
    wrapper?: React.JSXElementConstructor<{ children: React.ReactNode }>
  }
) => {
  const { routerProps = { initialEntries: ['/'] }, wrapper: Test, ...mountOptions } = options || {}

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
}
