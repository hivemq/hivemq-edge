import { useMutation, useQueryClient } from '@tanstack/react-query'

import type { ApiError, Combiner } from '@/api/__generated__'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'
import { QUERY_KEYS } from '@/api/utils.ts'

interface UpdateAssetMapperProps {
  combinerId: string
  requestBody: Combiner
}

export const useUpdateAssetMapper = () => {
  const appClient = useHttpClient()
  const queryClient = useQueryClient()

  const updateAssetMapper = ({ combinerId, requestBody }: UpdateAssetMapperProps) => {
    return appClient.pulse.updateAssetMapper(combinerId, requestBody)
  }

  return useMutation<UpdateAssetMapperProps, ApiError, UpdateAssetMapperProps>({
    mutationFn: updateAssetMapper,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [QUERY_KEYS.ASSET_MAPPER] })
      queryClient.invalidateQueries({ queryKey: [QUERY_KEYS.PULSE_ASSETS] })
    },
  })
}
