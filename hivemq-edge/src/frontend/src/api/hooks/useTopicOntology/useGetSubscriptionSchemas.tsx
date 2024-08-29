import { useQuery } from '@tanstack/react-query'
import { QUERY_KEYS } from '@/api/utils.ts'
import { RJSFSchema } from '@rjsf/utils'

import { ApiError } from '@/api/__generated__'
import { GENERATE_DATA_MODELS } from '@/api/hooks/useTopicOntology/__handlers__'

/**
 * @deprecated This is a mock, replace with https://hivemq.kanbanize.com/ctrl_board/57/cards/25661/details/
 */
export const useGetSubscriptionSchemas = (topic: string, adapter?: string) => {
  return useQuery<RJSFSchema, ApiError>({
    queryKey: [QUERY_KEYS.DISCOVERY_SCHEMAS, topic],
    queryFn: async () => {
      await new Promise((resolve) => setTimeout(resolve, 1000))
      return GENERATE_DATA_MODELS(Array.isArray(topic) ? topic.length : 1)
    },
    enabled: Boolean(adapter),
  })
}
