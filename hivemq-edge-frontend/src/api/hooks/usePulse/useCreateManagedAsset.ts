import { useMutation, useQueryClient } from '@tanstack/react-query'

import type { ApiError, ManagedAsset } from '@/api/__generated__'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'
import { QUERY_KEYS } from '@/api/utils.ts'

interface CreateManagedAssetProps {
  requestBody: ManagedAsset
}

export const useCreateManagedAsset = () => {
  const appClient = useHttpClient()
  const queryClient = useQueryClient()

  const createManagedAsset = ({ requestBody }: CreateManagedAssetProps) => {
    return appClient.pulse.addManagedAssets(requestBody)
  }

  return useMutation<CreateManagedAssetProps, ApiError, CreateManagedAssetProps>({
    mutationFn: createManagedAsset,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [QUERY_KEYS.PULSE_ASSETS] })
    },
  })
}
