import { useMutation, useQueryClient } from '@tanstack/react-query'

import type { ApiError, ManagedAsset } from '@/api/__generated__'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'
import { QUERY_KEYS } from '@/api/utils.ts'

interface UpdateManagedAssetProps {
  assetId: string
  requestBody: ManagedAsset
}

export const useUpdateManagedAsset = () => {
  const appClient = useHttpClient()
  const queryClient = useQueryClient()

  const updateManagedAsset = ({ assetId, requestBody }: UpdateManagedAssetProps) => {
    return appClient.pulse.updateManagedAssets(assetId, requestBody)
  }

  return useMutation<UpdateManagedAssetProps, ApiError, UpdateManagedAssetProps>({
    mutationFn: updateManagedAsset,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [QUERY_KEYS.PULSE_ASSETS] })
    },
  })
}
