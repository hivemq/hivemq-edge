import { useMutation, useQueryClient } from '@tanstack/react-query'
import { ApiError, ClientFilter, ClientFilterList } from '../../__generated__'

import { QUERY_KEYS } from '@/api/utils.ts'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'

export const useCreateClientSubscriptions = () => {
  const appClient = useHttpClient()
  const queryClient = useQueryClient()

  const addClientFilter = (client: ClientFilter) => {
    return appClient.client.addClientFilter(client)
  }

  return useMutation<ClientFilter, ApiError, ClientFilter>({
    mutationFn: addClientFilter,
    /**
     * @deprecated This is a mock, missing persistence from backend (https://hivemq.kanbanize.com/ctrl_board/57/cards/25322/details/)
     */
    onError: (_, data) => {
      /* istanbul ignore next -- @preserve */
      queryClient.setQueryData<ClientFilterList>([QUERY_KEYS.CLIENTS], (old) => {
        return Array.from(new Set([data, ...(old || [])]))
      })
    },
  })
}
