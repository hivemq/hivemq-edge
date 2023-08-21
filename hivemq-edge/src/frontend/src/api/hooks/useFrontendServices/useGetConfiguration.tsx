import { useQuery } from '@tanstack/react-query'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'
import { QUERY_KEYS } from '@/api/utils.ts'
import { ApiError, GatewayConfiguration } from '@/api/__generated__'

export const useGetConfiguration = () => {
  const appClient = useHttpClient()

  return useQuery<GatewayConfiguration, ApiError>([QUERY_KEYS.FRONTEND_CONFIGURATION], async () => {
    const item = await appClient.frontend.getConfiguration()
    return item
  })
}
