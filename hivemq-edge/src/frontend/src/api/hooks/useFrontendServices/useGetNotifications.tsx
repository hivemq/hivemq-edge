import { ApiError, NotificationList } from '@/api/__generated__'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'
import { QUERY_KEYS } from '@/api/utils.ts'
import { useQuery } from '@tanstack/react-query'

export const useGetNotifications = () => {
  const appClient = useHttpClient()

  return useQuery<NotificationList, ApiError>([QUERY_KEYS.FRONTEND_NOTIFICATION], async () => {
    const item = await appClient.frontend.getNotifications()
    return item
  })
}
