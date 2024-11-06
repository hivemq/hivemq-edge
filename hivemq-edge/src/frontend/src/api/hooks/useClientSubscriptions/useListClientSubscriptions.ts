import { useQuery } from '@tanstack/react-query'
import { QUERY_KEYS } from '@/api/utils.ts'

import { ApiError, type ClientFilterList } from '@/api/__generated__'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'

export const useListClientSubscriptions = () => {
  const appClient = useHttpClient()

  return useQuery<ClientFilterList, ApiError>({
    queryKey: [QUERY_KEYS.CLIENTS],
    queryFn: () => appClient.client.getClientFilters(),
    retry: false,
  })
}
