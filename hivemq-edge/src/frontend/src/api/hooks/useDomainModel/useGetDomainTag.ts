import { useQuery } from '@tanstack/react-query'
import { ApiError, type DomainTag } from '@/api/__generated__'

import { QUERY_KEYS } from '@/api/utils.ts'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'

export const useGetDomainTag = (tagName: string) => {
  const appClient = useHttpClient()

  return useQuery<DomainTag, ApiError>({
    queryKey: [QUERY_KEYS.DISCOVERY_TAGS, tagName],
    queryFn: () => appClient.protocolAdapters.getDomainTag(encodeURIComponent(tagName)),
  })
}
