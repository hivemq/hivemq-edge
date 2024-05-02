import { useQuery } from '@tanstack/react-query'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'
import config from '@/config'
import { QUERY_KEYS } from '@/api/utils.ts'

export const useGetBridgesStatus = () => {
  const appClient = useHttpClient()

  return useQuery({
    queryKey: [QUERY_KEYS.BRIDGES, QUERY_KEYS.CONNECTION_STATUS],
    queryFn: () => appClient.bridges.getBridgesStatus(),
    retry: 0,
    refetchInterval: config.httpClient.pollingRefetchInterval,
  })
}
