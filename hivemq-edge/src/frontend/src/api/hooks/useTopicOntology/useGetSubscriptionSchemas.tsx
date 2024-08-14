import { useQuery } from '@tanstack/react-query'
import { QUERY_KEYS } from '@/api/utils.ts'
import { RJSFSchema } from '@rjsf/utils'

import { ApiError } from '@/api/__generated__'

import { MOCK_MQTT_SCHEMA_REFS } from '@/__test-utils__/adapters/mqtt-subscription.mocks.ts'

/**
 * @deprecated This is a mock
 */
export const useGetSubscriptionSchemas = (topic: string, adapter?: string) => {
  return useQuery<RJSFSchema, ApiError>({
    queryKey: [QUERY_KEYS.DISCOVERY_SCHEMAS, topic],
    queryFn: async () => {
      await new Promise((resolve) => setTimeout(resolve, 1000))
      return MOCK_MQTT_SCHEMA_REFS
    },
    enabled: Boolean(adapter),
  })
}
