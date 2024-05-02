import { useQuery } from '@tanstack/react-query'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'

import config from '@/config'
import { QUERY_KEYS } from '@/api/utils.ts'

/**
 * @deprecated Prefer using useGetConnectionStatus
 */
export const useGetBridgeConnectionStatus = (name: string | undefined) => {
  const appClient = useHttpClient()

  return useQuery({
    queryKey: [QUERY_KEYS.BRIDGES, name, QUERY_KEYS.CONNECTION_STATUS],
    queryFn: () => appClient.bridges.getBridgeStatus(name as string),

    enabled: name !== undefined,
    retry: 0,
    refetchInterval: config.httpClient.pollingRefetchInterval,
  })
}
