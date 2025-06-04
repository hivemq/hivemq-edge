import { useMemo } from 'react'
import mqttTopicMatch from 'mqtt-match'
import type { Tree, TreeLeaf } from '@/modules/DomainOntology/types.ts'

import { useGetDomainOntology } from '@/modules/DomainOntology/hooks/useGetDomainOntology.ts'

export const useGetTreeData = () => {
  const { topicFilters, tags, northMappings, southMappings, bridgeSubscriptions, isLoading, isError } =
    useGetDomainOntology()

  const treeData = useMemo<Tree>(() => {
    const treeBridgeFilters =
      bridgeSubscriptions.topicFilters.map<Tree>((filter) => ({
        type: 'leaf',
        name: filter,
        value: 10,
        links: [],
      })) || []

    const treeBridgeTopics =
      bridgeSubscriptions.topics.map<Tree>((filter) => ({
        type: 'leaf',
        name: filter,
        value: 10,
        links: [],
      })) || []

    const treeFilters =
      topicFilters.data?.items?.map<Tree>((filter) => ({
        type: 'leaf',
        name: filter.topicFilter,
        value: 10,
        links: [],
      })) || []

    const treeTags =
      tags.data?.items?.map<Tree>((filter) => ({
        type: 'leaf',
        name: filter.name,
        value: 10,
        links: [],
      })) || []

    const topicTree =
      northMappings.data?.items?.map<Tree>((filter) => ({
        type: 'leaf',
        name: filter.topic,
        value: 10,
        links: [],
      })) || []

    for (const north of northMappings.data?.items || []) {
      const gg = treeTags.find((e) => e.name === north.tagName)
      if (gg) (gg as TreeLeaf).links.push(north.topic)
    }

    for (const south of southMappings.data?.items || []) {
      const gg = treeFilters.find((e) => e.name === south.topicFilter)
      if (gg && south.tagName) (gg as TreeLeaf).links.push(south.tagName)
    }

    for (const topic of topicTree) {
      for (const filter of treeFilters) {
        const gg = mqttTopicMatch(filter.name, topic.name)
        if (gg) (topic as TreeLeaf).links.push(filter.name)
      }
    }

    return {
      type: 'node',
      name: 'Edge',
      value: 0,
      children: [
        {
          type: 'node',
          name: 'Topic Filters',
          value: 10,
          children: [...treeFilters, ...treeBridgeFilters],
        },
        {
          type: 'node',
          name: 'Tags',
          value: 10,
          children: [...treeTags],
        },
        {
          type: 'node',
          name: 'Topics',
          value: 10,
          children: [...topicTree, ...treeBridgeTopics],
        },
      ],
    }
  }, [
    bridgeSubscriptions.topicFilters,
    bridgeSubscriptions.topics,
    northMappings.data?.items,
    southMappings.data?.items,
    tags.data?.items,
    topicFilters.data?.items,
  ])

  return {
    treeData,
    isLoading,
    isError,
  }
}
