import { useQuery } from '@tanstack/react-query'
import { QUERY_KEYS } from '@/api/utils.ts'

import { ApiError, type SouthboundMappingList } from '@/api/__generated__'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'

export const useListDomainSouthboundMappings = () => {
  const appClient = useHttpClient()

  return useQuery<SouthboundMappingList, ApiError>({
    queryKey: [QUERY_KEYS.SOUTHBOUND_MAPPINGS],
    queryFn: () => appClient.protocolAdapters.getSouthboundMappings(),
  })
}
