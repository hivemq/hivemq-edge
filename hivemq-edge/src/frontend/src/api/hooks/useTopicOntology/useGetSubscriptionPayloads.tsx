import { useQuery } from '@tanstack/react-query'
import { QUERY_KEYS } from '@/api/utils.ts'

import { ApiError, JsonNode } from '@/api/__generated__'

/**
 * @deprecated This is a mock, replace with https://hivemq.kanbanize.com/ctrl_board/57/cards/25257/details/
 */
export const useGetSubscriptionPayloads = (topic: string | string[], adapter?: string) => {
  return useQuery<JsonNode, ApiError>({
    queryKey: [QUERY_KEYS.DISCOVERY_PAYLOADS, topic],
    queryFn: async () => {
      await new Promise((resolve) => setTimeout(resolve, 1000))
      return []
    },
    enabled: Boolean(adapter),
  })
}
