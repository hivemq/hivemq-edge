import type { FC } from 'react'
// The DOM build of the provider; it is the root one with `flushSync: ReactDOM.flushSync` supplied,
// which is what makes `viewTransition`/`flushSync` navigations work. Everything else still comes
// from the framework-agnostic 'react-router' entry point.
import { RouterProvider } from 'react-router/dom'
import { QueryClientProvider } from '@tanstack/react-query'
import { ReactQueryDevtools } from '@tanstack/react-query-devtools'
import { ChakraProvider } from '@chakra-ui/react'

import '@fontsource/roboto/400.css'
import '@fontsource/roboto/700.css'

import queryClient from '@/api/queryClient.ts'
import { AccessibleDraggableProvider } from '@/hooks/useAccessibleDraggable'
import { routes } from '@/modules/App/routes.tsx'
import { AuthProvider } from '@/modules/Auth/AuthProvider.tsx'
import themeHiveMQ from '@/modules/Theme/themeHiveMQ.ts'
import PrivacyConsentBanner from '@/modules/Trackers/PrivacyConsentBanner.tsx'

import config from '@/config'

const MainApp: FC = () => {
  return (
    <QueryClientProvider client={queryClient}>
      <ChakraProvider theme={themeHiveMQ}>
        <AccessibleDraggableProvider>
          <AuthProvider>
            <RouterProvider router={routes} />
          </AuthProvider>
        </AccessibleDraggableProvider>
        <PrivacyConsentBanner />
      </ChakraProvider>
      {config.isDevMode && <ReactQueryDevtools position="bottom" buttonPosition="top-left" initialIsOpen={false} />}
    </QueryClientProvider>
  )
}

export default MainApp
