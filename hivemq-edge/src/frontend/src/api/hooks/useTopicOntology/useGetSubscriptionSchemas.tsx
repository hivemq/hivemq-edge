import { useQuery } from '@tanstack/react-query'
import { QUERY_KEYS } from '@/api/utils.ts'
import { RJSFSchema } from '@rjsf/utils'
import { inferSchema } from '@jsonhero/schema-infer'

import { ApiError } from '@/api/__generated__'
import { GENERATE_DATA_MODELS } from '@/api/hooks/useTopicOntology/__handlers__'
import { usePrivateMqttClient } from '@/hooks/usePrivateMqttClient/usePrivateMqttClient.ts'
import { MQTT_WILDCARD_MULTI } from '@/modules/Workspace/utils/topics-utils.ts'
import { useTranslation } from 'react-i18next'

/**s
 * @deprecated This is a mock, replace with https://hivemq.kanbanize.com/ctrl_board/57/cards/25661/details/
 */
export const useGetSubscriptionSchemas = (topic: string | string[], type?: 'source' | 'destination') => {
  const { t } = useTranslation()
  const mqttClient = usePrivateMqttClient()
  const allTopics = Array.isArray(topic) ? topic : [topic]
  allTopics.sort()
  const topicFilter = Array.isArray(topic) ? MQTT_WILDCARD_MULTI : topic

  return useQuery<Record<string, RJSFSchema>, ApiError>({
    // eslint-disable-next-line @tanstack/query/exhaustive-deps
    queryKey: [QUERY_KEYS.DISCOVERY_SCHEMAS, allTopics],
    queryFn: async () => {
      if (type === 'destination') return GENERATE_DATA_MODELS(type === 'destination')

      const samples = await mqttClient.actions?.onSampling(MQTT_WILDCARD_MULTI)
      if (!samples) throw new Error(t('domainMapping.error.noSampleForTopic', { topicFilter }))

      const results: Record<string, RJSFSchema> = {}
      for (const { topic, payload } of samples || []) {
        const inference = inferSchema(payload)
        if (allTopics.includes(topic)) {
          // The inference process doesn't deal with examples; values need a second pattern processing
          results[topic] = inference.toJSONSchema() as RJSFSchema
        }
      }
      return results
    },
    enabled: Boolean(type),
    retry: 1,
  })
}
