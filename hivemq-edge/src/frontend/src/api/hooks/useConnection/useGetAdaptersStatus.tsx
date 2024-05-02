import { useQuery } from '@tanstack/react-query'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'
import config from '@/config'
import { QUERY_KEYS } from '@/api/utils.ts'

export const useGetAdaptersStatus = () => {
  const appClient = useHttpClient()

  return useQuery({
    queryKey: [QUERY_KEYS.ADAPTERS, QUERY_KEYS.CONNECTION_STATUS],
    queryFn: () => appClient.protocolAdapters.getAdaptersStatus(),
    retry: 0,
    refetchInterval: config.httpClient.pollingRefetchInterval,
  })
}
