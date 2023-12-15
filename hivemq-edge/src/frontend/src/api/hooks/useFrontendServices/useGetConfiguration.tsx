import { ApiError, GatewayConfiguration } from '@/api/__generated__'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'
import { QUERY_KEYS } from '@/api/utils.ts'
import { useQuery } from '@tanstack/react-query'

export const useGetConfiguration = () => {
  const appClient = useHttpClient()

  return useQuery<GatewayConfiguration, ApiError>([QUERY_KEYS.FRONTEND_CONFIGURATION], async () => {
    const item = await appClient.frontend.getConfiguration()
    return item
  })
}
