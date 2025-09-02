import { useQuery } from '@tanstack/react-query'

import type { ApiError, Combiner, CombinerList } from '@/api/__generated__'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'
import { QUERY_KEYS } from '@/api/utils.ts'

export const useSelectCombinerFromMapping = (mappingId: string | undefined) => {
  const appClient = useHttpClient()

  return useQuery<CombinerList, ApiError, Combiner>({
    queryKey: [QUERY_KEYS.ADAPTERS, 'all', mappingId],
    queryFn: () => appClient.combiners.getCombiners(),
    enabled: mappingId !== undefined,
    select: (combiners) => {
      const combiner = combiners.items.find((c) => c.mappings.items.some((m) => m.id === mappingId))

      if (!combiner) {
        throw new Error('No matching Combiner found')
      }
      return combiner
    },
  })
}
