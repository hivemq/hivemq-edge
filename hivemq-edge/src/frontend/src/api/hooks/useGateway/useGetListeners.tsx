import { useQuery } from '@tanstack/react-query'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'
import { QUERY_KEYS } from '@/api/utils.ts'
import { ApiError, ListenerList } from '@/api/__generated__'

export const useGetListeners = () => {
  const appClient = useHttpClient()

  return useQuery<ListenerList, ApiError>({
    queryKey: [QUERY_KEYS.LISTENERS],
    queryFn: async () => {
      const item = await appClient.gatewayEndpoint.getListeners()
      return item
    },
  })
}
