import { useQuery } from '@tanstack/react-query'
import { QUERY_KEYS } from '@/api/utils.ts'

import type { ApiError, SouthboundMappingOwnerList } from '@/api/__generated__'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'

export const useListDomainSouthboundMappings = () => {
  const appClient = useHttpClient()

  return useQuery<SouthboundMappingOwnerList, ApiError>({
    queryKey: [QUERY_KEYS.SOUTHBOUND_MAPPINGS],
    queryFn: () => appClient.protocolAdapters.getSouthboundMappings(),
  })
}
