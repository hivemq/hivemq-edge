import { useQuery } from '@tanstack/react-query'
import { QUERY_KEYS } from '@/api/utils.ts'

import { ApiError } from '@/api/__generated__'
import { DeviceTags } from '@/modules/Subscriptions/types.ts'
import { MOCK_DEVICE_TAGS } from '@/api/hooks/useTopicOntology/__handlers__'

/**
 * @deprecated This is a mock, replace with https://hivemq.kanbanize.com/ctrl_board/57/cards/25736/details/
 */
export const useGetDeviceTags = (adapterId: string | undefined) => {
  return useQuery<DeviceTags[], ApiError>({
    queryKey: [QUERY_KEYS.ADAPTERS, adapterId, QUERY_KEYS.DISCOVERY_TAGS],
    queryFn: async () => {
      await new Promise((resolve) => setTimeout(resolve, 1000))
      return MOCK_DEVICE_TAGS
    },
    enabled: Boolean(adapterId),
  })
}
