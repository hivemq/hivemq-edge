import { useQuery } from '@tanstack/react-query'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'
import { QUERY_KEYS } from '@/api/utils.ts'
import type { ApiError, MetricList } from '@/api/__generated__'

export const useGetMetrics = () => {
  const appClient = useHttpClient()

  return useQuery<MetricList, ApiError>({
    queryKey: [QUERY_KEYS.METRICS],
    queryFn: () => appClient.metrics.getMetrics(),
  })
}
