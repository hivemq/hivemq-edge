import { useMutation, useQueryClient } from '@tanstack/react-query'
import { ApiError } from '../../__generated__'

import { QUERY_KEYS } from '@/api/utils.ts'
import { BrokerClient } from '@/api/types/api-broker-client.ts'

/**
 * @deprecated This is a mock, missing persistence from backend (https://hivemq.kanbanize.com/ctrl_board/57/cards/25322/details/)
 */
export const useDeleteClientSubscriptions = () => {
  const queryClient = useQueryClient()

  return useMutation<string, ApiError, string>({
    mutationFn: async (id: string) => {
      await new Promise((resolve) => setTimeout(resolve, 800))
      return id
    },
    onSuccess: (data) => {
      queryClient.setQueryData<BrokerClient[]>([QUERY_KEYS.CLIENTS], (old) => {
        const newClients = old?.filter((e) => e.id === data)

        return [...(newClients || [])]
      })
    },
  })
}
