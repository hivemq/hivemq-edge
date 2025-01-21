import { useMemo } from 'react'

import { stratifyTopicTree, toTreeMetadata } from '@/modules/Workspace/utils/topics-utils.ts'
import { useGetDomainOntology } from '@/modules/DomainOntology/hooks/useGetDomainOntology.ts'

export const useGetSunburstData = () => {
  const { topicFilters, tags, northMappings, bridgeSubscriptions, isLoading } = useGetDomainOntology()

  const sunburstData = useMemo(() => {
    if (isLoading) return stratifyTopicTree([{ label: 'root', count: 1 }])

    const allTopicFilters = topicFilters.data?.items?.map<string>((filter) => filter.topicFilter) || []
    const allTags = tags.data?.items?.map<string>((filter) => filter.name) || []
    const allTopics = northMappings.data?.items?.map<string>((filter) => filter.topic) || []

    const edgeTopics = toTreeMetadata([
      ...allTags,
      ...allTopics,
      ...allTopicFilters,
      ...bridgeSubscriptions.topics,
      ...bridgeSubscriptions.topicFilters,
    ])
    return stratifyTopicTree(edgeTopics)
  }, [
    bridgeSubscriptions.topicFilters,
    bridgeSubscriptions.topics,
    isLoading,
    northMappings.data?.items,
    tags.data?.items,
    topicFilters.data?.items,
  ])

  return {
    sunburstData: sunburstData,
    isLoading: isLoading,
  }
}
