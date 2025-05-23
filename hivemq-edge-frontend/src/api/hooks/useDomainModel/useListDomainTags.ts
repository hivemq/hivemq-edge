import { useQuery } from '@tanstack/react-query'
import { QUERY_KEYS } from '@/api/utils.ts'

import type { ApiError } from '@/api/__generated__'
import { type DomainTagList } from '@/api/__generated__'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'

export const useListDomainTags = () => {
  const appClient = useHttpClient()

  return useQuery<DomainTagList, ApiError>({
    queryKey: [QUERY_KEYS.DISCOVERY_TAGS],
    queryFn: () => appClient.protocolAdapters.getDomainTags(),
  })
}
