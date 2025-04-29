import { useQuery } from '@tanstack/react-query'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'
import type { ApiError } from '@/api/__generated__'
import { type NorthboundMappingList } from '@/api/__generated__'
import { QUERY_KEYS } from '@/api/utils.ts'

export const useListNorthboundMappings = (adapterId: string) => {
  const appClient = useHttpClient()

  return useQuery<NorthboundMappingList, ApiError>({
    queryKey: [QUERY_KEYS.ADAPTERS, adapterId, QUERY_KEYS.NORTHBOUND_MAPPINGS],
    queryFn: () => appClient.protocolAdapters.getAdapterNorthboundMappings(adapterId),
  })
}
