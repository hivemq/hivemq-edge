import { useQuery } from '@tanstack/react-query'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'
import type { ApiError } from '@/api/__generated__'
import { type DomainTagList } from '@/api/__generated__'
import { QUERY_KEYS } from '@/api/utils.ts'

export const useGetDomainTags = (adapterId: string) => {
  const appClient = useHttpClient()

  return useQuery<DomainTagList, ApiError>({
    queryKey: [QUERY_KEYS.ADAPTERS, adapterId, QUERY_KEYS.DISCOVERY_TAGS],
    queryFn: () => appClient.protocolAdapters.getAdapterDomainTags(adapterId),
  })
}
