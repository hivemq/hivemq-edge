import { useMemo } from 'react'
import type { DefaultLink, DefaultNode, SankeyDataProps } from '@nivo/sankey'

import { useGetDomainOntology } from '@/modules/DomainOntology/hooks/useGetDomainOntology.ts'

export const useGetSankeyData = () => {
  const { topicFilters, tags, northMappings, bridgeSubscriptions, southMappings, isLoading, isError } =
    useGetDomainOntology()

  const sankeyData = useMemo(() => {
    const allTopicFilters = topicFilters.data?.items?.map<string>((filter) => filter.topicFilter) || []
    const allTags = tags.data?.items?.map<string>((filter) => filter.name) || []
    const allTopics = northMappings.data?.items?.map<string>((filter) => filter.topic) || []

    const links: DefaultLink[] = []
    for (const north of northMappings.data?.items || []) {
      const x = allTags.findIndex((key) => key === north.tagName)
      const y = allTopics.findIndex((key) => key === north.topic)
      if (x !== -1 && y !== -1) {
        links.push({
          source: north.tagName,
          target: north.topic,
          value: 1,
        })
      }
    }

    for (const south of southMappings.data?.items || []) {
      const x = allTopicFilters.findIndex((key) => key === south.topicFilter)
      const y = allTags.findIndex((key) => key === south.tagName)
      if (x !== -1 && y !== -1 && south.topicFilter && south.tagName) {
        links.push({
          source: south.topicFilter,
          target: south.tagName,
          value: 1,
        })
      }
    }

    for (const bridgeSubs of bridgeSubscriptions.mappings || []) {
      // const x = allTopicFilters.findIndex((key) => key === bridgeSubs[0])
      // const y = allTopicFilters.findIndex((key) => key === bridgeSubs[1])
      // if (x !== -1 && y !== -1)
      {
        links.push({
          source: bridgeSubs[0],
          target: bridgeSubs[1],
          value: 1,
        })
      }
    }

    // for (const south of southMappings.data?.items || []) {
    //   const x = keys.findIndex((key) => key === south.topicFilter)
    //   const y = keys.findIndex((key) => key === south.tagName)
    //   setAdjacencyMatrix(x, y)
    // }

    const data: SankeyDataProps<DefaultNode, DefaultLink> = {
      data: {
        nodes: [
          ...allTopics,
          ...allTags,
          ...allTopicFilters,
          ...bridgeSubscriptions.topics,
          ...bridgeSubscriptions.topicFilters,
        ].map((e) => ({ id: e })),
        links: links,
      },
    }
    return data
  }, [
    bridgeSubscriptions.mappings,
    bridgeSubscriptions.topicFilters,
    bridgeSubscriptions.topics,
    northMappings.data?.items,
    southMappings.data?.items,
    tags.data?.items,
    topicFilters.data?.items,
  ])

  return {
    sankeyData,
    isLoading,
    isError,
  }
}
