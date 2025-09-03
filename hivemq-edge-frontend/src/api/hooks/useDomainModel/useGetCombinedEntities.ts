import type { QueryFunction, QueryKey } from '@tanstack/react-query'
import { useQueries } from '@tanstack/react-query'

import type { EntityReference } from '@/api/__generated__'
import { EntityType } from '@/api/__generated__'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'
import { QUERY_KEYS } from '@/api/utils.ts'

export const useGetCombinedEntities = (entities: EntityReference[]) => {
  const appClient = useHttpClient()

  // TODO[NVL] Likely too many duplicates; needs filtering
  const combinedQueries = entities.reduce(
    (acc, entity) => {
      if (entity.type === EntityType.ADAPTER) {
        acc.push({
          queryKey: [QUERY_KEYS.ADAPTERS, entity.id, QUERY_KEYS.DISCOVERY_TAGS],
          queryFn: () => appClient.protocolAdapters.getAdapterDomainTags(entity.id),
        })
      } else if (entity.type === EntityType.BRIDGE || entity.type === EntityType.EDGE_BROKER) {
        // TODO[NVL] Certainly a hack: Bridges are not supported yet
        acc.push({
          queryKey: [QUERY_KEYS.DISCOVERY_TOPIC_FILTERS],
          queryFn: () => appClient.topicFilters.getTopicFilters(),
        })
      }
      return acc
    },
    [] as Array<{ queryKey: QueryKey; queryFn: QueryFunction }>
  )

  return useQueries({
    queries: combinedQueries,
  })
}
