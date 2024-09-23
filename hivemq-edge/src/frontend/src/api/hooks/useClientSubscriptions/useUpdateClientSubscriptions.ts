import { useMutation, useQueryClient } from '@tanstack/react-query'
import { ApiError, ClientFilter, type ClientFilterList } from '../../__generated__'

import { QUERY_KEYS } from '@/api/utils.ts'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'

export const useUpdateClientSubscriptions = () => {
  const appClient = useHttpClient()
  const queryClient = useQueryClient()

  const updateClientFilter = (client: ClientFilter) => {
    return appClient.client.updateClientFilter(client.id, client)
  }

  return useMutation<ClientFilter, ApiError, ClientFilter>({
    mutationFn: updateClientFilter,
    /**
     * @deprecated This is a mock, missing persistence from backend (https://hivemq.kanbanize.com/ctrl_board/57/cards/25322/details/)
     */
    onError: (_, data) => {
      /* istanbul ignore next -- @preserve */
      queryClient.setQueryData<ClientFilterList>([QUERY_KEYS.CLIENTS], (old) => {
        const index = old?.findIndex((client) => client.id === data.id)
        if (index !== undefined && old) {
          old[index] = data
        }

        return [...(old || [])]
      })
    },
  })
}
