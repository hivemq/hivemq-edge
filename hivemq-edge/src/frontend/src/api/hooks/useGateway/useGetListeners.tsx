import { useQuery } from '@tanstack/react-query'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'
import { QUERY_KEYS } from '@/api/utils.ts'
import { ApiError, ListenerList } from '@/api/__generated__'

export const useGetListeners = () => {
  const appClient = useHttpClient()

  return useQuery<ListenerList, ApiError>([QUERY_KEYS.LISTENERS], async () => {
    const item = await appClient.gatewayEndpoint.getListeners()
    return item
  })
}
