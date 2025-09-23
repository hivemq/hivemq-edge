import { useMutation, useQueryClient } from '@tanstack/react-query'

import type { ApiError } from '@/api/__generated__'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'
import { QUERY_KEYS } from '@/api/utils.ts'

export const useDeleteManagedAsset = () => {
  const appClient = useHttpClient()
  const queryClient = useQueryClient()

  const deleteManagedAsset = (assetId: string) => {
    return appClient.pulse.deleteManagedAsset(assetId)
  }

  return useMutation<string, ApiError, string>({
    mutationFn: deleteManagedAsset,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [QUERY_KEYS.PULSE_ASSETS] })
    },
  })
}
