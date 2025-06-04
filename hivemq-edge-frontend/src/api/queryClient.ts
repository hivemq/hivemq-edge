import { QueryClient } from '@tanstack/react-query'
import config from '@/config'

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 1000 * 60 * 5, // 5 minutes
      refetchOnWindowFocus: false,
      refetchOnReconnect: false,
      refetchOnMount: true,
      networkMode: config.httpClient.networkMode,
    },
    mutations: {
      networkMode: config.httpClient.networkMode,
    },
  },
})

export default queryClient
