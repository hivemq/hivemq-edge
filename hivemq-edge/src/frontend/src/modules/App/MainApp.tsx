import { ChakraProvider } from '@chakra-ui/react'
import { FC } from 'react'
import { RouterProvider } from 'react-router-dom'

import { themeHiveMQ } from '@/modules/Theme/themeHiveMQ.ts'

import { QueryClientProvider } from '@tanstack/react-query'
import { ReactQueryDevtools } from '@tanstack/react-query-devtools'
import queryClient from '../../api/queryClient.ts'
import { AuthProvider } from '../Auth/AuthProvider.tsx'
import { routes } from './routes.tsx'

const MainApp: FC = () => {
  return (
    <QueryClientProvider client={queryClient}>
      <ChakraProvider theme={themeHiveMQ}>
        <AuthProvider>
          <RouterProvider router={routes} />
        </AuthProvider>
      </ChakraProvider>
      {import.meta.env.MODE === 'development' && <ReactQueryDevtools position={'bottom-right'} initialIsOpen={false} />}
    </QueryClientProvider>
  )
}

export default MainApp
