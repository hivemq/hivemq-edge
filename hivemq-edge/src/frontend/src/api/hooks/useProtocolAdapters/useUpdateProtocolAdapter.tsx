import { useMutation, useQueryClient } from '@tanstack/react-query'
import { Adapter, ApiError } from '../../__generated__'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'
import { QUERY_KEYS } from '@/api/utils.ts'

interface UpdateBridgeProps {
  adapterId: string
  requestBody: Adapter
}

export const useUpdateProtocolAdapter = () => {
  const appClient = useHttpClient()
  const queryClient = useQueryClient()

  const updateProtocolAdapter = ({ adapterId, requestBody }: UpdateBridgeProps) => {
    return appClient.protocolAdapters.updateAdapter(adapterId, requestBody)
  }

  return useMutation<unknown, ApiError, UpdateBridgeProps>(updateProtocolAdapter, {
    onSuccess: () => {
      queryClient.invalidateQueries([QUERY_KEYS.ADAPTERS])
    },
  })
}
