import { useMutation, useQueryClient } from '@tanstack/react-query'

import type { ApiError, Combiner } from '@/api/__generated__'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'
import { QUERY_KEYS } from '@/api/utils.ts'

interface CreateAssetMapperProps {
  requestBody: Combiner
}

export const useCreateAssetMapper = () => {
  const appClient = useHttpClient()
  const queryClient = useQueryClient()

  const createCombiner = ({ requestBody }: CreateAssetMapperProps) => {
    return appClient.pulse.addAssetMapper(requestBody)
  }

  return useMutation<unknown, ApiError, CreateAssetMapperProps>({
    mutationFn: createCombiner,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [QUERY_KEYS.ASSET_MAPPER] })
      queryClient.invalidateQueries({ queryKey: [QUERY_KEYS.PULSE_ASSETS] })
    },
  })
}
