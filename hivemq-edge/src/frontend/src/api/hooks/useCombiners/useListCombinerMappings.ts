import { useQuery } from '@tanstack/react-query'

import type { ApiError, DataCombiningList } from '@/api/__generated__'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'
import { QUERY_KEYS } from '@/api/utils.ts'

export const useListCombinerMappings = (combinerId: string) => {
  const appClient = useHttpClient()

  return useQuery<DataCombiningList, ApiError>({
    queryKey: [QUERY_KEYS.COMBINER, combinerId, QUERY_KEYS.COMBINER_MAPPINGS],
    queryFn: () => appClient.combiners.getCombinerMappings(combinerId),
  })
}
