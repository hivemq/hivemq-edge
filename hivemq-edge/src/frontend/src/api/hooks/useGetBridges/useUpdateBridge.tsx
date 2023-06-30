import { useMutation, useQueryClient } from '@tanstack/react-query'
import { ApiError, Bridge } from '../../__generated__'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'
import { QUERY_KEYS } from '@/api/utils.ts'

interface UpdateBridgeProps {
  name: string
  requestBody: Bridge
}

export const useUpdateBridge = () => {
  const appClient = useHttpClient()
  const queryClient = useQueryClient()

  const updateBridge = ({ name, requestBody }: UpdateBridgeProps) => {
    return appClient.bridges.updateBridge(name, requestBody)
  }

  return useMutation<unknown, ApiError, UpdateBridgeProps>(updateBridge, {
    onSuccess: () => {
      queryClient.invalidateQueries([QUERY_KEYS.BRIDGES])
    },
  })
}
