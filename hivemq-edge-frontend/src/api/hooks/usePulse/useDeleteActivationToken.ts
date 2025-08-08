import { useMutation, useQueryClient } from '@tanstack/react-query'

import type { ApiError } from '@/api/__generated__'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'
import { QUERY_KEYS } from '@/api/utils.ts'

export const useDeleteActivationToken = () => {
  const appClient = useHttpClient()
  const queryClient = useQueryClient()

  const deleteCombiner = () => {
    return appClient.pulse.deletePulseActivationToken()
  }

  return useMutation<unknown, ApiError>({
    mutationFn: deleteCombiner,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [QUERY_KEYS.FRONTEND_CAPABILITIES] })
    },
  })
}
