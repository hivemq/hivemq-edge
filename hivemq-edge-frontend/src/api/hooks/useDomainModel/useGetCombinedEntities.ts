import type { QueryFunction, QueryKey, UseQueryResult } from '@tanstack/react-query'
import { useQueries } from '@tanstack/react-query'

import type { DomainTagList, EntityReference, TopicFilterList } from '@/api/__generated__'
import { EntityType } from '@/api/__generated__'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'
import { QUERY_KEYS } from '@/api/utils.ts'

export const useGetCombinedEntities = (
  entities: EntityReference[]
): UseQueryResult<DomainTagList | TopicFilterList, Error>[] => {
  const appClient = useHttpClient()

  // Build deduplicated queries
  const uniqueQueries: Array<{ queryKey: QueryKey; queryFn: QueryFunction<DomainTagList | TopicFilterList> }> = []
  const queryKeyToIndex = new Map<string, number>()

  // Create entity-to-query-index mapping
  const entityQueryIndices: number[] = []

  entities.forEach((entity) => {
    if (entity.type === EntityType.ADAPTER) {
      // Each adapter has its own unique query
      const queryKey = [QUERY_KEYS.ADAPTERS, entity.id, QUERY_KEYS.DISCOVERY_TAGS]
      const queryKeyStr = JSON.stringify(queryKey)

      let queryIndex = queryKeyToIndex.get(queryKeyStr)
      if (queryIndex === undefined) {
        queryIndex = uniqueQueries.length
        uniqueQueries.push({
          queryKey,
          queryFn: () => appClient.protocolAdapters.getAdapterDomainTags(entity.id),
        })
        queryKeyToIndex.set(queryKeyStr, queryIndex)
      }
      entityQueryIndices.push(queryIndex)
    } else if (entity.type === EntityType.BRIDGE || entity.type === EntityType.EDGE_BROKER) {
      // Bridges and edge brokers share the same topic filters query
      const queryKey = [QUERY_KEYS.DISCOVERY_TOPIC_FILTERS]
      const queryKeyStr = JSON.stringify(queryKey)

      let queryIndex = queryKeyToIndex.get(queryKeyStr)
      if (queryIndex === undefined) {
        queryIndex = uniqueQueries.length
        uniqueQueries.push({
          queryKey,
          queryFn: () => appClient.topicFilters.getTopicFilters(),
        })
        queryKeyToIndex.set(queryKeyStr, queryIndex)
      }
      entityQueryIndices.push(queryIndex)
    } else {
      // PULSE_AGENT and other types don't provide integration points
      // Return an empty result to maintain array length (prevents undefined queries)
      const queryKey = ['empty', entity.type, entity.id]
      const queryKeyStr = JSON.stringify(queryKey)

      let queryIndex = queryKeyToIndex.get(queryKeyStr)
      if (queryIndex === undefined) {
        queryIndex = uniqueQueries.length
        uniqueQueries.push({
          queryKey,
          queryFn: () => Promise.resolve({ items: [] } as DomainTagList),
        })
        queryKeyToIndex.set(queryKeyStr, queryIndex)
      }
      entityQueryIndices.push(queryIndex)
    }
  })

  // Execute unique queries
  const queryResults = useQueries({
    queries: uniqueQueries,
  })

  // Map results back to match entities array order
  return entityQueryIndices.map((queryIndex) => queryResults[queryIndex])
}
