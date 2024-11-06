import { useMutation, useQueryClient } from '@tanstack/react-query'
import { ApiError, ClientFilterList } from '../../__generated__'

import { QUERY_KEYS } from '@/api/utils.ts'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'

export const useDeleteClientSubscriptions = () => {
  const appClient = useHttpClient()
  const queryClient = useQueryClient()

  const deleteClientFilter = (id: string) => {
    return appClient.client.deleteClientFilter(id)
  }

  return useMutation<string, ApiError, string>({
    mutationFn: deleteClientFilter,
    /**
     * @deprecated This is a mock, missing persistence from backend (https://hivemq.kanbanize.com/ctrl_board/57/cards/25322/details/)
     */
    onSuccess: (data) => {
      queryClient.setQueryData<ClientFilterList>([QUERY_KEYS.CLIENTS], (old) => {
        const newClients = old?.filter((client) => client.id === data)

        return [...(newClients || [])]
      })
    },
  })
}
