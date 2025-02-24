import { useQuery } from '@tanstack/react-query'

import type { ApiError, CombinerList } from '@/api/__generated__'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'
import { QUERY_KEYS } from '@/api/utils.ts'

export const useListCombiners = () => {
  const appClient = useHttpClient()

  return useQuery<CombinerList, ApiError>({
    queryKey: [QUERY_KEYS.COMBINER],
    queryFn: () => appClient.combiners.getCombiners(),
  })
}
