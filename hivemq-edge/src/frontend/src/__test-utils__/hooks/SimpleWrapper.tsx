import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { AuthProvider } from '@/modules/Auth/AuthProvider.tsx'
import { MemoryRouter } from 'react-router-dom'

export const SimpleWrapper: React.JSXElementConstructor<{ children: React.ReactElement }> = ({ children }) => (
  <QueryClientProvider
    client={
      new QueryClient({
        defaultOptions: {
          queries: {
            retry: false,
          },
        },
      })
    }
  >
    <AuthProvider>
      <MemoryRouter>{children}</MemoryRouter>
    </AuthProvider>
  </QueryClientProvider>
)
