import { useMutation, useQueryClient } from '@tanstack/react-query'

import type { ApiError, CancelablePromise, PulseActivationToken } from '@/api/__generated__'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'
import { QUERY_KEYS } from '@/api/utils.ts'

export const useCreateActivationToken = () => {
  const appClient = useHttpClient()
  const queryClient = useQueryClient()

  const mutationFn = (token: PulseActivationToken): CancelablePromise<{ type: number }> => {
    return appClient.pulse.updatePulseActivationToken(token)
  }

  return useMutation<unknown, ApiError, PulseActivationToken>({
    mutationFn,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [QUERY_KEYS.FRONTEND_CAPABILITIES] })
    },
  })
}
