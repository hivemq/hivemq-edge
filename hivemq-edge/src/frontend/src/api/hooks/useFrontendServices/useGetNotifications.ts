import { useQuery } from '@tanstack/react-query'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'
import { QUERY_KEYS } from '@/api/utils.ts'
import type { ApiError, NotificationList } from '@/api/__generated__'

export const useGetNotifications = () => {
  const appClient = useHttpClient()

  return useQuery<NotificationList, ApiError>({
    queryKey: [QUERY_KEYS.FRONTEND_NOTIFICATION],
    queryFn: () => appClient.frontend.getNotifications(),
  })
}
