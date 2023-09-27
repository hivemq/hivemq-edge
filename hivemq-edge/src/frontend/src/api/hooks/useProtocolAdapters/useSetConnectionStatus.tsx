import { useMutation, useQueryClient } from '@tanstack/react-query'
import { ApiError, StatusTransitionCommand } from '../../__generated__'

import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'
import { QUERY_KEYS } from '@/api/utils.ts'

interface SetConnectionStatusProps {
  adapterId: string
  requestBody: StatusTransitionCommand
}

export const useSetConnectionStatus = () => {
  const appClient = useHttpClient()
  const queryClient = useQueryClient()

  const changeStatus = ({ adapterId, requestBody }: SetConnectionStatusProps) => {
    return appClient.protocolAdapters.changeStatus1(adapterId, requestBody)
  }

  return useMutation<unknown, ApiError, SetConnectionStatusProps>(changeStatus, {
    onSuccess: () => {
      queryClient.invalidateQueries([QUERY_KEYS.ADAPTERS])
    },
  })
}
