import { FC } from 'react'
import { RouterProvider } from 'react-router-dom'
import { QueryClientProvider } from '@tanstack/react-query'
import { ReactQueryDevtools } from '@tanstack/react-query-devtools'
import { ChakraProvider } from '@chakra-ui/react'

import '@fontsource/roboto/400.css'
import '@fontsource/roboto/700.css'

import queryClient from '@/api/queryClient.ts'
import { routes } from '@/modules/App/routes.tsx'
import { AuthProvider } from '@/modules/Auth/AuthProvider.tsx'
import themeHiveMQ from '@/modules/Theme/themeHiveMQ.ts'
import PrivacyConsentBanner from '@/modules/Trackers/PrivacyConsentBanner.tsx'

const MainApp: FC = () => {
  return (
    <QueryClientProvider client={queryClient}>
      <ChakraProvider theme={themeHiveMQ}>
        <AuthProvider>
          <RouterProvider router={routes} />
        </AuthProvider>
        <PrivacyConsentBanner />
      </ChakraProvider>
      {import.meta.env.MODE === 'development' && <ReactQueryDevtools position="bottom" initialIsOpen={false} />}
    </QueryClientProvider>
  )
}

export default MainApp
