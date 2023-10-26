import { useQuery } from '@tanstack/react-query'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'
import { ApiError, EventList } from '@/api/__generated__'
import { QUERY_KEYS } from '@/api/utils.ts'
import config from '@/config'

export const useGetEvents = () => {
  const appClient = useHttpClient()

  return useQuery<EventList, ApiError>(
    [QUERY_KEYS.EVENTS],
    async () => {
      return await appClient.events.getEvents()
    },
    {
      retry: 0,
      refetchInterval: () => {
        return config.httpClient.pollingRefetchInterval
      },
    }
  )
}
