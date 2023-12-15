import { ApiError, EventList } from '@/api/__generated__'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'
import { QUERY_KEYS } from '@/api/utils.ts'
import { useQuery } from '@tanstack/react-query'

export const useGetEvents = () => {
  const appClient = useHttpClient()

  return useQuery<EventList, ApiError>([QUERY_KEYS.EVENTS], async () => {
    return await appClient.events.getEvents()
  })
}
