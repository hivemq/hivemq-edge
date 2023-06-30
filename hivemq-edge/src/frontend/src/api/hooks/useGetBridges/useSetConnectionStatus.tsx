import { useMutation, useQueryClient } from '@tanstack/react-query'
import { ApiError, ConnectionStatusTransitionCommand } from '../../__generated__'

import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'
import { QUERY_KEYS } from '@/api/utils.ts'

interface SetConnectionStatusProps {
  name: string
  requestBody: ConnectionStatusTransitionCommand
}

export const useSetConnectionStatus = () => {
  const appClient = useHttpClient()
  const queryClient = useQueryClient()

  const setConnectionStatus = ({ name, requestBody }: SetConnectionStatusProps) => {
    return appClient.bridges.changeConnectionStatus(name, requestBody)
  }

  return useMutation<string, ApiError, SetConnectionStatusProps>(setConnectionStatus, {
    onSuccess: () => {
      // queryClient.invalidateQueries(['bridges', variables.name, 'connection.status'])
      queryClient.invalidateQueries([QUERY_KEYS.BRIDGES])
    },
  })
}
