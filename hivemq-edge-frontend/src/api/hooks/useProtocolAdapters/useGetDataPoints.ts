import { useQuery } from '@tanstack/react-query'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'
import type { ApiError, ValuesTree } from '@/api/__generated__'
import { QUERY_KEYS } from '@/api/utils.ts'

export const useGetDataPoints = (
  isDiscoverable: boolean,
  adapterId: string | undefined,
  root?: string,
  depth?: number
) => {
  const appClient = useHttpClient()

  return useQuery<ValuesTree, ApiError>({
    queryKey: [QUERY_KEYS.ADAPTERS, adapterId, QUERY_KEYS.DISCOVERY_POINTS, root, depth],
    queryFn: () => appClient.protocolAdapters.discoverDataPoints(adapterId || '', root, depth),
    enabled: Boolean(isDiscoverable && adapterId),
  })
}
