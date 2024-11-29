import { useQuery } from '@tanstack/react-query'

import { ApiError, type FieldMappingsListModel } from '@/api/__generated__'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'
import { QUERY_KEYS } from '@/api/utils.ts'

export const useGetAdapterFieldMappings = (adapterId: string) => {
  const appClient = useHttpClient()

  return useQuery<FieldMappingsListModel, ApiError>({
    queryKey: [QUERY_KEYS.ADAPTERS, adapterId, QUERY_KEYS.DISCOVERY_FIELD_MAPPINGS],
    queryFn: () => appClient.protocolAdapters.getAdapterFieldMappings(adapterId),
  })
}
