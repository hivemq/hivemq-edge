import { useQuery } from '@tanstack/react-query'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'
import type { ApiError, EventList } from '@/api/__generated__'
import { QUERY_KEYS } from '@/api/utils.ts'

export const useGetEvents = () => {
  const appClient = useHttpClient()

  return useQuery<EventList, ApiError>({
    queryKey: [QUERY_KEYS.EVENTS],
    queryFn: () => appClient.events.getEvents(),
  })
}
