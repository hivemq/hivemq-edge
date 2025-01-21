import { useMemo } from 'react'
import mqttTopicMatch from 'mqtt-match'

import type { DomainTag, NorthboundMapping, TopicFilter } from '@/api/__generated__'
import { useGetDomainOntology } from '@/modules/DomainOntology/hooks/useGetDomainOntology.ts'

export const useGetChordMatrixData = () => {
  const { topicFilters, tags, northMappings, bridgeSubscriptions, southMappings, isLoading, isError } =
    useGetDomainOntology()

  const matrixData = useMemo(() => {
    // const allTopicFilters = topicFilters.data?.items?.map<string>((filter) => filter.topicFilter) || []
    // const allTags = tags.data?.items?.map<string>((filter) => filter.name) || []
    // const allTopics = northMappings.data?.items?.map<string>((filter) => filter.topic) || []

    type Datum = Record<string, TopicFilter | DomainTag | NorthboundMapping>
    let datum: Datum = {}
    datum =
      topicFilters.data?.items.reduce<Datum>((acc, curr) => {
        acc[curr.topicFilter] = curr
        return acc
      }, datum) || datum

    datum =
      tags.data?.items.reduce<Datum>((acc, curr) => {
        acc[curr.name] = curr
        return acc
      }, datum) || datum

    datum =
      northMappings.data?.items.reduce<Datum>((acc, curr) => {
        acc[curr.topic] = curr
        return acc
      }, datum) || datum

    const keys = Array.from(
      new Set([...Object.keys(datum), ...bridgeSubscriptions.topics, ...bridgeSubscriptions.topicFilters])
    ) //Array.from(new Set([...allTags, ...allTopics, ...allTopicFilters]))
    const matrix = Array.from(Array(keys.length), () => Array.from(Array(keys.length), () => 0))

    console.log(
      'XXXXXXX ke',
      keys,
      datum,
      Object.values(datum).map((e) => typeof e)
    )
    const setAdjacencyMatrix = (x: number, y: number) => {
      if (x !== -1 && y !== -1) {
        matrix[x][x] += 0
        matrix[y][y] += 0
        matrix[x][y] += 1
        matrix[y][x] += 3
      }
    }
    for (const north of northMappings.data?.items || []) {
      const x = keys.findIndex((key) => key === north.tagName)
      const y = keys.findIndex((key) => key === north.topic)
      setAdjacencyMatrix(x, y)
    }

    for (const south of southMappings.data?.items || []) {
      const x = keys.findIndex((key) => key === south.topicFilter)
      const y = keys.findIndex((key) => key === south.tagName)
      setAdjacencyMatrix(x, y)
    }

    for (const bridgeSubs of bridgeSubscriptions.mappings || []) {
      const x = keys.findIndex((key) => key === bridgeSubs[0])
      const y = keys.findIndex((key) => key === bridgeSubs[1])
      setAdjacencyMatrix(x, y)
    }

    for (const topic of keys) {
      for (const filter of topicFilters.data?.items || []) {
        const gg = mqttTopicMatch(filter.topicFilter, topic)
        if (gg && filter.topicFilter !== topic) {
          const x = keys.findIndex((key) => key === topic)
          const y = keys.findIndex((key) => key === filter.topicFilter)
          setAdjacencyMatrix(x, y)
        }
      }
    }

    return {
      matrix: matrix,
      keys: keys,
    }
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
    matrixData,
    isLoading,
    isError,
  }
}
