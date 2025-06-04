import { useQuery } from '@tanstack/react-query'

import type { ApiError, Instruction } from '@/api/__generated__'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'
import { QUERY_KEYS } from '@/api/utils.ts'

export const useListCombinerMappingInstructions = (combinerId: string, mappingId: string) => {
  const appClient = useHttpClient()

  return useQuery<Array<Instruction>, ApiError>({
    queryKey: [
      QUERY_KEYS.COMBINER,
      combinerId,
      QUERY_KEYS.COMBINER_MAPPINGS,
      mappingId,
      QUERY_KEYS.COMBINER_MAPPINGS_INSTRUCTIONS,
    ],
    queryFn: () => appClient.combiners.getMappingInstructions(combinerId, mappingId),
  })
}
