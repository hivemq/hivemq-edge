import { useQueries } from '@tanstack/react-query'

import { QUERY_KEYS } from '@/api/utils.ts'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'
import type { SchemaHandler } from '@/modules/TopicFilters/utils/topic-filter.schema'

export enum DataReferenceType {
  TAG = 'TAG',
  TOPIC_FILTER = 'TOPIC_FILTER',
}

export type DataReference = {
  id: string
  type: DataReferenceType
  adapterId?: string
  schema?: SchemaHandler
}

export const useGetCombinedDataSchemas = (dataIdentifiers: DataReference[]) => {
  const appClient = useHttpClient()

  return useQueries({
    queries: dataIdentifiers.map((dataPoint) => {
      return dataPoint.type === DataReferenceType.TAG
        ? {
            queryKey: [QUERY_KEYS.ADAPTERS, dataPoint.adapterId, QUERY_KEYS.DISCOVERY_TAGS, dataPoint.id],
            queryFn: () =>
              appClient.protocolAdapters.getWritingSchema(
                dataPoint.adapterId as string,
                encodeURIComponent(dataPoint.id)
              ),
          }
        : {
            // TODO[NVL] Certainly a hack: returns topic filters. Bridges are not supported yet
            queryKey: [QUERY_KEYS.DISCOVERY_TOPIC_FILTERS, dataPoint.id, QUERY_KEYS.DISCOVERY_SCHEMAS],
            queryFn: async () => {
              const { items } = await appClient.topicFilters.getTopicFilters()
              // TODO[NVL] Definitely a hack
              return items.find((e) => e.topicFilter === dataPoint.id)?.schema
            },
          }
    }),
  })
}
