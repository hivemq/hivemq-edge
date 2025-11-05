import { useMemo } from 'react'
import type { Edge, Node } from '@xyflow/react'
import { MarkerType } from '@xyflow/react'

import { useGetDomainOntology } from '@/modules/DomainOntology/hooks/useGetDomainOntology.ts'

export type NetworkGraphNodeType = 'TAG' | 'TOPIC' | 'TOPIC_FILTER'
export type NetworkGraphEdgeType = 'NORTHBOUND' | 'SOUTHBOUND' | 'BRIDGE' | 'COMBINER' | 'ASSET_MAPPER'

export interface NetworkGraphNodeData {
  label: string
  type: NetworkGraphNodeType
  status?: 'active' | 'inactive' | 'error'
  connectionCount: number
  metadata: Record<string, unknown>
  [key: string]: unknown
}

export interface NetworkGraphEdgeData {
  transformationType: NetworkGraphEdgeType
  label?: string
  metadata: Record<string, unknown>
  [key: string]: unknown
}

export type NetworkGraphNode = Node<NetworkGraphNodeData>
export type NetworkGraphEdge = Edge<NetworkGraphEdgeData>

export const useGetNetworkGraphData = () => {
  const {
    topicFilters,
    tags,
    northMappings,
    southMappings,
    bridgeSubscriptions,
    combiners,
    assetMappers,
    isLoading,
    isError,
  } = useGetDomainOntology()

  const { nodes, edges } = useMemo(() => {
    const nodesMap = new Map<string, NetworkGraphNode>()
    const edgesList: NetworkGraphEdge[] = []

    // Create nodes for all TAGs
    for (const tag of tags.data?.items || []) {
      const nodeId = `tag-${tag.name}`
      nodesMap.set(nodeId, {
        id: nodeId,
        type: 'networkGraphNode',
        position: { x: 0, y: 0 }, // Will be positioned by layout algorithm
        data: {
          label: tag.name,
          type: 'TAG',
          connectionCount: 0,
          metadata: { tagName: tag.name },
        },
      })
    }

    // Create nodes for all TOPICs and count connections
    const topicSet = new Set<string>()

    // Add topics from northbound mappings
    for (const mapping of northMappings.data?.items || []) {
      topicSet.add(mapping.topic)
    }

    // Add topics from bridge subscriptions
    for (const topic of bridgeSubscriptions.topics || []) {
      topicSet.add(topic)
    }

    for (const topic of topicSet) {
      const nodeId = `topic-${topic}`
      nodesMap.set(nodeId, {
        id: nodeId,
        type: 'networkGraphNode',
        position: { x: 0, y: 0 },
        data: {
          label: topic,
          type: 'TOPIC',
          connectionCount: 0,
          metadata: {},
        },
      })
    }

    // Create nodes for all TOPIC FILTERs
    for (const filter of topicFilters.data?.items || []) {
      const nodeId = `filter-${filter.topicFilter}`
      nodesMap.set(nodeId, {
        id: nodeId,
        type: 'networkGraphNode',
        position: { x: 0, y: 0 },
        data: {
          label: filter.topicFilter,
          type: 'TOPIC_FILTER',
          connectionCount: 0,
          metadata: { description: filter.description },
        },
      })
    }

    // Create edges for northbound mappings (TAG → TOPIC)
    for (const mapping of northMappings.data?.items || []) {
      const sourceId = `tag-${mapping.tagName}`
      const targetId = `topic-${mapping.topic}`

      if (nodesMap.has(sourceId) && nodesMap.has(targetId)) {
        // Increment connection counts
        const sourceNode = nodesMap.get(sourceId)!
        const targetNode = nodesMap.get(targetId)!
        sourceNode.data.connectionCount++
        targetNode.data.connectionCount++

        edgesList.push({
          id: `north-${mapping.tagName}-${mapping.topic}`,
          source: sourceId,
          target: targetId,
          type: 'networkGraphEdge',
          markerEnd: { type: MarkerType.ArrowClosed },
          data: {
            transformationType: 'NORTHBOUND',
            label: 'Northbound',
            metadata: { maxQoS: mapping.maxQoS },
          },
        })
      }
    }

    // Create edges for southbound mappings (TOPIC FILTER → TAG)
    for (const mapping of southMappings.data?.items || []) {
      const sourceId = `filter-${mapping.topicFilter}`
      const targetId = `tag-${mapping.tagName}`

      if (nodesMap.has(sourceId) && nodesMap.has(targetId)) {
        const sourceNode = nodesMap.get(sourceId)!
        const targetNode = nodesMap.get(targetId)!
        sourceNode.data.connectionCount++
        targetNode.data.connectionCount++

        edgesList.push({
          id: `south-${mapping.topicFilter}-${mapping.tagName}`,
          source: sourceId,
          target: targetId,
          type: 'networkGraphEdge',
          markerEnd: { type: MarkerType.ArrowClosed },
          data: {
            transformationType: 'SOUTHBOUND',
            label: 'Southbound',
            metadata: { topicFilter: mapping.topicFilter, tagName: mapping.tagName },
          },
        })
      }
    }

    // Create edges for bridge subscriptions (TOPIC ↔ TOPIC)
    for (const [sourceFilter, targetTopic] of bridgeSubscriptions.mappings || []) {
      const sourceId = `filter-${sourceFilter}`
      const targetId = `topic-${targetTopic}`

      // Check if nodes exist, create filter node if needed
      if (!nodesMap.has(sourceId)) {
        nodesMap.set(sourceId, {
          id: sourceId,
          type: 'networkGraphNode',
          position: { x: 0, y: 0 },
          data: {
            label: sourceFilter,
            type: 'TOPIC_FILTER',
            connectionCount: 0,
            metadata: { fromBridge: true },
          },
        })
      }

      if (nodesMap.has(targetId)) {
        const sourceNode = nodesMap.get(sourceId)!
        const targetNode = nodesMap.get(targetId)!
        sourceNode.data.connectionCount++
        targetNode.data.connectionCount++

        edgesList.push({
          id: `bridge-${sourceFilter}-${targetTopic}`,
          source: sourceId,
          target: targetId,
          type: 'networkGraphEdge',
          markerEnd: { type: MarkerType.ArrowClosed },
          data: {
            transformationType: 'BRIDGE',
            label: 'Bridge',
            metadata: {},
          },
        })
      }
    }

    // Create edges for combiners (TAG/TOPIC_FILTER → TOPIC)
    for (const combiner of combiners.data?.items || []) {
      for (const mapping of combiner.mappings?.items || []) {
        const destinationTopic = mapping.destination?.topic
        if (!destinationTopic) continue

        const targetId = `topic-${destinationTopic}`

        // Add destination topic node if it doesn't exist
        if (!nodesMap.has(targetId)) {
          topicSet.add(destinationTopic)
          nodesMap.set(targetId, {
            id: targetId,
            type: 'networkGraphNode',
            position: { x: 0, y: 0 },
            data: {
              label: destinationTopic,
              type: 'TOPIC',
              connectionCount: 0,
              metadata: { fromCombiner: true },
            },
          })
        }

        // Create edges from tags
        for (const tagName of mapping.sources?.tags || []) {
          const sourceId = `tag-${tagName}`
          if (nodesMap.has(sourceId)) {
            const sourceNode = nodesMap.get(sourceId)!
            const targetNode = nodesMap.get(targetId)!
            sourceNode.data.connectionCount++
            targetNode.data.connectionCount++

            edgesList.push({
              id: `combiner-${combiner.id}-${tagName}-${destinationTopic}`,
              source: sourceId,
              target: targetId,
              type: 'networkGraphEdge',
              markerEnd: { type: MarkerType.ArrowClosed },
              data: {
                transformationType: 'COMBINER',
                label: 'Combiner',
                metadata: { combinerId: combiner.id, combinerName: combiner.name },
              },
            })
          }
        }

        // Create edges from topic filters
        for (const filterName of mapping.sources?.topicFilters || []) {
          const sourceId = `filter-${filterName}`
          if (nodesMap.has(sourceId)) {
            const sourceNode = nodesMap.get(sourceId)!
            const targetNode = nodesMap.get(targetId)!
            sourceNode.data.connectionCount++
            targetNode.data.connectionCount++

            edgesList.push({
              id: `combiner-${combiner.id}-${filterName}-${destinationTopic}`,
              source: sourceId,
              target: targetId,
              type: 'networkGraphEdge',
              markerEnd: { type: MarkerType.ArrowClosed },
              data: {
                transformationType: 'COMBINER',
                label: 'Combiner',
                metadata: { combinerId: combiner.id, combinerName: combiner.name },
              },
            })
          }
        }
      }
    }

    // Create edges for asset mappers (TAG/TOPIC_FILTER → TOPIC)
    for (const assetMapper of assetMappers.data?.items || []) {
      for (const mapping of assetMapper.mappings?.items || []) {
        const destinationTopic = mapping.destination?.topic
        if (!destinationTopic) continue

        const targetId = `topic-${destinationTopic}`

        // Add destination topic node if it doesn't exist
        if (!nodesMap.has(targetId)) {
          topicSet.add(destinationTopic)
          nodesMap.set(targetId, {
            id: targetId,
            type: 'networkGraphNode',
            position: { x: 0, y: 0 },
            data: {
              label: destinationTopic,
              type: 'TOPIC',
              connectionCount: 0,
              metadata: { fromAssetMapper: true },
            },
          })
        }

        // Create edges from tags
        for (const tagName of mapping.sources?.tags || []) {
          const sourceId = `tag-${tagName}`
          if (nodesMap.has(sourceId)) {
            const sourceNode = nodesMap.get(sourceId)!
            const targetNode = nodesMap.get(targetId)!
            sourceNode.data.connectionCount++
            targetNode.data.connectionCount++

            edgesList.push({
              id: `asset-mapper-${assetMapper.id}-${tagName}-${destinationTopic}`,
              source: sourceId,
              target: targetId,
              type: 'networkGraphEdge',
              markerEnd: { type: MarkerType.ArrowClosed },
              data: {
                transformationType: 'ASSET_MAPPER',
                label: 'Asset Mapper',
                metadata: { assetMapperId: assetMapper.id, assetMapperName: assetMapper.name },
              },
            })
          }
        }

        // Create edges from topic filters
        for (const filterName of mapping.sources?.topicFilters || []) {
          const sourceId = `filter-${filterName}`
          if (nodesMap.has(sourceId)) {
            const sourceNode = nodesMap.get(sourceId)!
            const targetNode = nodesMap.get(targetId)!
            sourceNode.data.connectionCount++
            targetNode.data.connectionCount++

            edgesList.push({
              id: `asset-mapper-${assetMapper.id}-${filterName}-${destinationTopic}`,
              source: sourceId,
              target: targetId,
              type: 'networkGraphEdge',
              markerEnd: { type: MarkerType.ArrowClosed },
              data: {
                transformationType: 'ASSET_MAPPER',
                label: 'Asset Mapper',
                metadata: { assetMapperId: assetMapper.id, assetMapperName: assetMapper.name },
              },
            })
          }
        }
      }
    }

    return {
      nodes: Array.from(nodesMap.values()),
      edges: edgesList,
    }
  }, [
    tags.data?.items,
    topicFilters.data?.items,
    northMappings.data?.items,
    southMappings.data?.items,
    bridgeSubscriptions.topics,
    bridgeSubscriptions.mappings,
    combiners.data?.items,
    assetMappers.data?.items,
  ])

  return {
    nodes,
    edges,
    isLoading,
    isError,
  }
}
