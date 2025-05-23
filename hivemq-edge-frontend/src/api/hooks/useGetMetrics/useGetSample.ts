import { useQuery } from '@tanstack/react-query'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'
import { QUERY_KEYS } from '@/api/utils.ts'
import type { ApiError, DataPoint } from '@/api/__generated__'
import config from '@/config'

export const useGetSample = (metricName: string | undefined) => {
  const appClient = useHttpClient()

  return useQuery<DataPoint, ApiError>({
    queryKey: [QUERY_KEYS.METRICS_SAMPLE, metricName],
    queryFn: () => appClient.metrics.getSample(metricName as string),

    enabled: !!metricName,
    retry: 0,
    refetchInterval: config.httpClient.pollingRefetchInterval,
  })
}
