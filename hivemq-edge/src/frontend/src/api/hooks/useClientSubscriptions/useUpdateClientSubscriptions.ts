import { useMutation, useQueryClient } from '@tanstack/react-query'
import { ApiError } from '../../__generated__'

import { QUERY_KEYS } from '@/api/utils.ts'
import { BrokerClient, BrokerClientConfiguration } from '@/api/types/api-broker-client.ts'

interface UpdateProtocolAdapterProps {
  id: string
  config: BrokerClientConfiguration
}

/**
 * @deprecated This is a mock, missing persistence from backend (https://hivemq.kanbanize.com/ctrl_board/57/cards/25322/details/)
 */
export const useUpdateClientSubscriptions = () => {
  const queryClient = useQueryClient()

  return useMutation<UpdateProtocolAdapterProps, ApiError, UpdateProtocolAdapterProps>({
    mutationFn: async ({ id, config }: UpdateProtocolAdapterProps) => {
      await new Promise((resolve) => setTimeout(resolve, 800))
      return { id, config }
    },
    onSuccess: (data) => {
      queryClient.setQueryData<BrokerClient[]>([QUERY_KEYS.CLIENTS], (old) => {
        const index = old?.findIndex((client) => client.id === data.id)
        if (index !== undefined && old) {
          old[index].config = data.config
        }

        return [...(old || [])]
      })
    },
  })
}
