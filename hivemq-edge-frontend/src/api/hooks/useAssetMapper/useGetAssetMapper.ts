import { useQuery } from '@tanstack/react-query'

import type { ApiError, Combiner } from '@/api/__generated__'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'
import { QUERY_KEYS } from '@/api/utils.ts'

export const useGetAssetMapper = (combinerId: string) => {
  const appClient = useHttpClient()

  return useQuery<Combiner, ApiError>({
    queryKey: [QUERY_KEYS.COMBINER, combinerId],
    queryFn: () => appClient.pulse.getAssetMapper(combinerId),
  })
}
