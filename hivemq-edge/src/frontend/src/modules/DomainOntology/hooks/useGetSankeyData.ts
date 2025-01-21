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

    console.log('XXXXXXXX links', links)

    // for (const south of southMappings.data?.items || []) {
    //   const x = keys.findIndex((key) => key === south.topicFilter)
    //   const y = keys.findIndex((key) => key === south.tagName)
    //   setAdjacencyMatrix(x, y)
    // }

    const gg: SankeyDataProps<DefaultNode, DefaultLink> = {
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
      // data: {
      //   nodes: [
      //     {
      //       id: 'John',
      //       nodeColor: 'hsl(245, 70%, 50%)',
      //     },
      //     {
      //       id: 'Raoul',
      //       nodeColor: 'hsl(168, 70%, 50%)',
      //     },
      //     {
      //       id: 'Jane',
      //       nodeColor: 'hsl(300, 70%, 50%)',
      //     },
      //     {
      //       id: 'Marcel',
      //       nodeColor: 'hsl(299, 70%, 50%)',
      //     },
      //     {
      //       id: 'Ibrahim',
      //       nodeColor: 'hsl(253, 70%, 50%)',
      //     },
      //     {
      //       id: 'Junko',
      //       nodeColor: 'hsl(12, 70%, 50%)',
      //     },
      //   ],
      //   links: [
      //     {
      //       source: 'Raoul',
      //       target: 'Junko',
      //       value: 179,
      //     },
      //     {
      //       source: 'Jane',
      //       target: 'Raoul',
      //       value: 126,
      //     },
      //     {
      //       source: 'Jane',
      //       target: 'Ibrahim',
      //       value: 124,
      //     },
      //     {
      //       source: 'Jane',
      //       target: 'Marcel',
      //       value: 5,
      //     },
      //     {
      //       source: 'Ibrahim',
      //       target: 'John',
      //       value: 155,
      //     },
      //     {
      //       source: 'Ibrahim',
      //       target: 'Junko',
      //       value: 110,
      //     },
      //     {
      //       source: 'John',
      //       target: 'Junko',
      //       value: 1,
      //     },
      //     {
      //       source: 'John',
      //       target: 'Marcel',
      //       value: 1,
      //     },
      //     {
      //       source: 'John',
      //       target: 'Raoul',
      //       value: 1,
      //     },
      //   ],
      // },
    }
    return gg
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
