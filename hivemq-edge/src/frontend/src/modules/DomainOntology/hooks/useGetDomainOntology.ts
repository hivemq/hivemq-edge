import { useMemo } from 'react'

import { useListDomainNorthboundMappings } from '@/api/hooks/useDomainModel/useListDomainNorthboundMappings.ts'
import { useListDomainSouthboundMappings } from '@/api/hooks/useDomainModel/useListDomainSouthboundMappings.ts'
import { useListDomainTags } from '@/api/hooks/useDomainModel/useListDomainTags.ts'
import { useListTopicFilters } from '@/api/hooks/useTopicFilters/useListTopicFilters.ts'
import { useListBridges } from '@/api/hooks/useGetBridges/useListBridges.ts'

export const useGetDomainOntology = () => {
  const northMappings = useListDomainNorthboundMappings()
  const southMappings = useListDomainSouthboundMappings()
  const tags = useListDomainTags()
  const topicFilters = useListTopicFilters()
  const bridges = useListBridges()

  const isLoading =
    northMappings.isLoading || southMappings.isLoading || tags.isLoading || bridges.isLoading || topicFilters.isLoading

  const bridgeSubscriptions = useMemo(() => {
    const topics = [] as string[]
    const topicFilters = [] as string[]
    const mappings = [] as string[][]

    for (const bridge of bridges.data || []) {
      for (const local of bridge.localSubscriptions || []) {
        const { destination, filters } = local
        topics.push(destination)
        topicFilters.push(...filters)
        mappings.push(...filters.map((e) => [e, destination]))
      }

      for (const remote of bridge.remoteSubscriptions || []) {
        const { destination, filters } = remote
        topics.push(destination)
        topicFilters.push(...filters)
        mappings.push(...filters.map((e) => [e, destination]))
      }
    }
    return { topics: Array.from(new Set(topics)), topicFilters: Array.from(new Set(topicFilters)), mappings }
  }, [bridges])

  return {
    topicFilters,
    tags,
    northMappings,
    southMappings,
    bridgeSubscriptions,
    isLoading: isLoading,
  }
}
