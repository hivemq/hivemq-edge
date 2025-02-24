import { useQueries } from '@tanstack/react-query'
import type { EntityReference } from '@/api/__generated__'
import { EntityType } from '@/api/__generated__'

import { QUERY_KEYS } from '@/api/utils.ts'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'

export const useGetCombinedEntities = (entities: EntityReference[]) => {
  const appClient = useHttpClient()

  // TODO[NVL] Likely too many duplicates; needs filtering
  return useQueries({
    queries: entities.map((entity) => {
      return entity.type === EntityType.ADAPTER
        ? {
            queryKey: [QUERY_KEYS.ADAPTERS, entity.id, QUERY_KEYS.DISCOVERY_TAGS],
            queryFn: () => appClient.protocolAdapters.getAdapterDomainTags(entity.id),
          }
        : {
            // TODO[NVL] Certainly s hack: everything else returns topic filters. Bridges are not supported yet
            queryKey: [QUERY_KEYS.DISCOVERY_TOPIC_FILTERS],
            queryFn: () => appClient.topicFilters.getTopicFilters(),
          }
    }),
  })
}
