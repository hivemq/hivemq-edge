import { useMutation, useQueryClient } from '@tanstack/react-query'
import { ApiError, Bridge } from '../../__generated__'

import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'
import { QUERY_KEYS } from '@/api/utils.ts'

export const useCreateBridge = () => {
  const appClient = useHttpClient()
  const queryClient = useQueryClient()

  const createBridge = (requestBody: Bridge) => {
    return appClient.bridges.addBridge(requestBody)
  }

  return useMutation<unknown, ApiError, Bridge>(createBridge, {
    onSuccess: () => {
      queryClient.invalidateQueries([QUERY_KEYS.BRIDGES])
    },
  })
}
