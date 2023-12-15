import { ApiError, ListenerList } from '@/api/__generated__'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'
import { QUERY_KEYS } from '@/api/utils.ts'
import { useQuery } from '@tanstack/react-query'

export const useGetListeners = () => {
  const appClient = useHttpClient()

  return useQuery<ListenerList, ApiError>([QUERY_KEYS.LISTENERS], async () => {
    const item = await appClient.gatewayEndpoint.getListeners()
    return item
  })
}
