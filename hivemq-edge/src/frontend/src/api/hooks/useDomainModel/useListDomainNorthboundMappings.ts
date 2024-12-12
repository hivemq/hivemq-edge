import { useQuery } from '@tanstack/react-query'
import { QUERY_KEYS } from '@/api/utils.ts'

import { ApiError, type NorthboundMappingList } from '@/api/__generated__'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'

export const useListDomainNorthboundMappings = () => {
  const appClient = useHttpClient()

  return useQuery<NorthboundMappingList, ApiError>({
    queryKey: [QUERY_KEYS.NORTHBOUND_MAPPINGS],
    queryFn: () => appClient.protocolAdapters.getNorthboundMappings(),
  })
}
