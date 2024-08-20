import { useMutation, useQueryClient } from '@tanstack/react-query'
import { ApiError } from '../../__generated__'

import { QUERY_KEYS } from '@/api/utils.ts'
import { BrokerClient, BrokerClientConfiguration } from '@/api/types/api-broker-client.ts'

interface CreateProtocolAdapterProps {
  id: string
  config: BrokerClientConfiguration
}

/**
 * @deprecated This is a mock, missing persistence from backend (https://hivemq.kanbanize.com/ctrl_board/57/cards/25322/details/)
 */
export const useCreateClientSubscriptions = () => {
  const queryClient = useQueryClient()

  return useMutation<CreateProtocolAdapterProps, ApiError, CreateProtocolAdapterProps>({
    mutationFn: async ({ id, config }: CreateProtocolAdapterProps) => {
      await new Promise((resolve) => setTimeout(resolve, 800))
      return { id, config }
    },
    onSuccess: (data) => {
      queryClient.setQueryData<BrokerClient[]>([QUERY_KEYS.CLIENTS], (old) => {
        const newClient: BrokerClient = {
          config: {
            ...data.config,
            id: data.id,
          },
          id: data.id,
          type: 'broker-client',
        }
        return Array.from(new Set([newClient, ...(old || [])]))
      })
    },
  })
}
