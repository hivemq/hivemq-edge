import { useQuery } from '@tanstack/react-query'

import type { ApiError, Combiner } from '@/api/__generated__'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'
import { QUERY_KEYS } from '@/api/utils.ts'

export const useGetCombiner = (combinerId: string) => {
  const appClient = useHttpClient()

  return useQuery<Combiner, ApiError>({
    queryKey: [QUERY_KEYS.ADAPTERS, combinerId],
    queryFn: () => appClient.combiners.getCombinersById(combinerId),
  })
}
