import { useMutation, useQueryClient } from '@tanstack/react-query'
import { ApiError } from '../../__generated__'

import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'
import { QUERY_KEYS } from '@/api/utils.ts'

export const useDeleteProtocolAdapter = () => {
  const appClient = useHttpClient()
  const queryClient = useQueryClient()

  const deleteAdapter = (adapterId: string) => {
    return appClient.protocolAdapters.deleteAdapter(adapterId)
  }

  return useMutation<string, ApiError, string>(deleteAdapter, {
    onSuccess: () => {
      queryClient.invalidateQueries([QUERY_KEYS.ADAPTERS])
    },
  })
}
