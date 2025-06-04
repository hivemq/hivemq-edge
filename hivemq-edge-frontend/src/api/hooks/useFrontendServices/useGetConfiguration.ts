import { useQuery } from '@tanstack/react-query'
import { useSimpleHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'
import { QUERY_KEYS } from '@/api/utils.ts'
import type { ApiError, GatewayConfiguration } from '@/api/__generated__'

export const useGetConfiguration = () => {
  const appClient = useSimpleHttpClient()

  return useQuery<GatewayConfiguration, ApiError>({
    queryKey: [QUERY_KEYS.FRONTEND_CONFIGURATION],
    queryFn: () => appClient.frontend.getConfiguration(),
  })
}
