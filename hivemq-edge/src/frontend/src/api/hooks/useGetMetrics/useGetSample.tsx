import { useQuery } from '@tanstack/react-query'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'
import { QUERY_KEYS } from '@/api/utils.ts'
import { ApiError, DataPoint } from '@/api/__generated__'
import config from '@/config'

export const useGetSample = (metricName: string | undefined) => {
  const appClient = useHttpClient()

  return useQuery<DataPoint, ApiError>(
    [QUERY_KEYS.METRICS_SAMPLE, metricName],
    async () => {
      const dataPoint = await appClient.metrics.getSample(metricName as string)
      return dataPoint
    },
    {
      enabled: !!metricName,
      retry: 0,
      refetchInterval: () => {
        // return data ? 4 * 1000 : Math.max(Math.min(query.state.errorUpdateCount, 5 * 60), 4) * 1000
        return config.httpClient.pollingRefetchInterval * 2
      },
    }
  )
}
