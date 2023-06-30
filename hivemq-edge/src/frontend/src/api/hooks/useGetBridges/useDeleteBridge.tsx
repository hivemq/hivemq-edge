import { useMutation, useQueryClient } from '@tanstack/react-query'
import { ApiError } from '../../__generated__'

import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'
import { QUERY_KEYS } from '@/api/utils.ts'

export const useDeleteBridge = () => {
  const appClient = useHttpClient()
  const queryClient = useQueryClient()

  const deleteBridge = (name: string) => {
    return appClient.bridges.removeBridge(name)
  }

  return useMutation<string, ApiError, string>(deleteBridge, {
    onSuccess: () => {
      queryClient.invalidateQueries([QUERY_KEYS.BRIDGES])
    },
  })
}
