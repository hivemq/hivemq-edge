import { ApiError, MetricList } from '@/api/__generated__'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'
import { QUERY_KEYS } from '@/api/utils.ts'
import { useQuery } from '@tanstack/react-query'

export const useGetMetrics = () => {
  const appClient = useHttpClient()

  return useQuery<MetricList, ApiError>([QUERY_KEYS.METRICS], async () => {
    const items = await appClient.metrics.getMetrics()
    return items
  })
}
