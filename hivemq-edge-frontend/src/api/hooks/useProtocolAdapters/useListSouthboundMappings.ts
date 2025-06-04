import { useQuery } from '@tanstack/react-query'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'
import type { ApiError } from '@/api/__generated__'
import { type SouthboundMappingList } from '@/api/__generated__'
import { QUERY_KEYS } from '@/api/utils.ts'

export const useListSouthboundMappings = (adapterId: string) => {
  const appClient = useHttpClient()

  return useQuery<SouthboundMappingList, ApiError>({
    queryKey: [QUERY_KEYS.ADAPTERS, adapterId, QUERY_KEYS.SOUTHBOUND_MAPPINGS],
    queryFn: () => appClient.protocolAdapters.getAdapterSouthboundMappings(adapterId),
  })
}
