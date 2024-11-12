import { useQuery } from '@tanstack/react-query'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'
import { ApiError, type DomainTagList } from '@/api/__generated__'
import { QUERY_KEYS } from '@/api/utils.ts'

/**
 * @deprecated This is a mock-based, will be reverted
 * @param adapterId
 * @param adapterType This is part of the mock, in order to identify the adapter
 */
export const useGetDomainTags = (adapterId: string | undefined, adapterType?: string) => {
  const appClient = useHttpClient()

  return useQuery<DomainTagList, ApiError>({
    // eslint-disable-next-line @tanstack/query/exhaustive-deps
    queryKey: [QUERY_KEYS.ADAPTERS, adapterId, QUERY_KEYS.DISCOVERY_TAGS],
    queryFn: () => appClient.protocolAdapters.getAdapterDomainTags(adapterId || '', adapterType),
    enabled: Boolean(adapterId),
  })
}
