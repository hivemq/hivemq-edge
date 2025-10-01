import { useMutation, useQueryClient } from '@tanstack/react-query'

import type { ApiError } from '@/api/__generated__'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'
import { QUERY_KEYS } from '@/api/utils.ts'

interface DeleteAssetMapperProps {
  combinerId: string
}

export const useDeleteAssetMapper = () => {
  const appClient = useHttpClient()
  const queryClient = useQueryClient()

  const deleteAssetMapper = ({ combinerId }: DeleteAssetMapperProps) => {
    return appClient.pulse.deleteAssetMapper(combinerId)
  }

  return useMutation<unknown, ApiError, DeleteAssetMapperProps>({
    mutationFn: deleteAssetMapper,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [QUERY_KEYS.ASSET_MAPPER] })
      queryClient.invalidateQueries({ queryKey: [QUERY_KEYS.PULSE_ASSETS] })
    },
  })
}
