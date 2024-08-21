import { useQuery } from '@tanstack/react-query'
import { QUERY_KEYS } from '@/api/utils.ts'

import { ApiError } from '@/api/__generated__'
import { BrokerClient } from '@/api/types/api-broker-client.ts'

/**
 * @deprecated This is a mock, missing persistence from backend (https://hivemq.kanbanize.com/ctrl_board/57/cards/25322/details/)
 */
export const useListClientSubscriptions = () => {
  return useQuery<BrokerClient[], ApiError>({
    queryKey: [QUERY_KEYS.CLIENTS],
    queryFn: async () => {
      await new Promise((resolve) => setTimeout(resolve, 500))
      return []
    },
  })
}
