import { useQuery } from '@tanstack/react-query'
import { QUERY_KEYS } from '@/api/utils.ts'

import { ApiError } from '@/api/__generated__'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'

export const useListDomainTags = () => {
  const appClient = useHttpClient()

  return useQuery<Array<string>, ApiError>({
    queryKey: [QUERY_KEYS.DISCOVERY_TAGS],
    queryFn: () => appClient.domain.getDomainTags(),
  })
}
