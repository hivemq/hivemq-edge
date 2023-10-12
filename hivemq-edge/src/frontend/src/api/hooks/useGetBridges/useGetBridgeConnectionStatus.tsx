import { useQuery } from '@tanstack/react-query'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'

import config from '@/config'
import { QUERY_KEYS } from '@/api/utils.ts'

/**
 * @deprecated Prefer using useGetConnectionStatus
 */
export const useGetBridgeConnectionStatus = (name: string | undefined) => {
  const appClient = useHttpClient()

  return useQuery(
    [QUERY_KEYS.BRIDGES, name, QUERY_KEYS.CONNECTION_STATUS],
    async () => {
      const item = await appClient.bridges.getBridgeStatus(name as string)
      return item
    },
    {
      enabled: name !== undefined,
      retry: 0,
      refetchInterval: () => {
        // return data ? 4 * 1000 : Math.max(Math.min(query.state.errorUpdateCount, 5 * 60), 4) * 1000
        return config.httpClient.pollingRefetchInterval
      },
    }
  )
}
