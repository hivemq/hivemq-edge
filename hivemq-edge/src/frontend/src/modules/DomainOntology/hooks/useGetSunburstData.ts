import { useMemo } from 'react'
import { useTranslation } from 'react-i18next'

import { stratifyTopicTree, toTreeMetadata } from '@/modules/Workspace/utils/topics-utils.ts'
import { useGetDomainOntology } from '@/modules/DomainOntology/hooks/useGetDomainOntology.ts'

export const useGetSunburstData = () => {
  const { t } = useTranslation()
  const { topicFilters, tags, northMappings, bridgeSubscriptions, isLoading, isError } = useGetDomainOntology()

  const sunburstData = useMemo(() => {
    // TODO[NVL] id and label needs to be added to datum when stratifying
    // The Sunburst chart doesn't allow a single sector
    const emptyStateData = stratifyTopicTree([
      { label: t('ontology.error.noDataLoaded'), count: 1 },
      { label: t('ontology.error.noDataLoaded'), count: 1 },
    ])

    if (isLoading) return emptyStateData

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

    if (!edgeTopics.length) return emptyStateData

    return stratifyTopicTree(edgeTopics)
  }, [
    bridgeSubscriptions.topicFilters,
    bridgeSubscriptions.topics,
    isLoading,
    northMappings.data?.items,
    t,
    tags.data?.items,
    topicFilters.data?.items,
  ])

  return {
    sunburstData,
    isLoading,
    isError,
  }
}
