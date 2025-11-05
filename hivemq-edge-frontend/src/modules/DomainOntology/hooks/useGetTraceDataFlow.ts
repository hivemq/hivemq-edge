import { useMemo } from 'react'

import { useGetDomainOntology } from '@/modules/DomainOntology/hooks/useGetDomainOntology.ts'

export type GraphNodeType = 'TAG' | 'TOPIC' | 'TOPIC_FILTER'
export type GraphEdgeType = 'NORTHBOUND' | 'SOUTHBOUND' | 'BRIDGE' | 'COMBINER' | 'ASSET_MAPPER'
export type TraceDirection = 'UPSTREAM' | 'DOWNSTREAM' | 'BIDIRECTIONAL'

export interface GraphNode {
  id: string
  type: GraphNodeType
  label: string
}

export interface GraphEdge {
  from: string
  to: string
  type: GraphEdgeType
  metadata?: Record<string, unknown>
}

export interface TraceHop {
  node: GraphNode
  edge?: GraphEdge
  hopNumber: number
}

export interface TraceResult {
  path: TraceHop[]
  direction: TraceDirection
  hasCycles: boolean
  startNode: GraphNode
  endNodes: GraphNode[]
}

/**
 * Hook to trace data flow through the domain ontology graph
 */
export const useGetTraceDataFlow = () => {
  const { tags, topicFilters, northMappings, southMappings, bridgeSubscriptions, combiners, assetMappers, isLoading } =
    useGetDomainOntology()

  // Build graph structure from domain ontology data
  const graph = useMemo(() => {
    const nodes = new Map<string, GraphNode>()
    const edges: GraphEdge[] = []

    // Create nodes for TAGs
    for (const tag of tags.data?.items || []) {
      nodes.set(`tag-${tag.name}`, {
        id: `tag-${tag.name}`,
        type: 'TAG',
        label: tag.name,
      })
    }

    // Create nodes for TOPICs (from various sources)
    const topicSet = new Set<string>()

    // Topics from northbound mappings
    for (const mapping of northMappings.data?.items || []) {
      topicSet.add(mapping.topic)
    }

    // Topics from bridge subscriptions
    for (const topic of bridgeSubscriptions.topics || []) {
      topicSet.add(topic)
    }

    // Topics from combiners
    for (const combiner of combiners.data?.items || []) {
      for (const mapping of combiner.mappings?.items || []) {
        if (mapping.destination?.topic) {
          topicSet.add(mapping.destination.topic)
        }
      }
    }

    // Topics from asset mappers
    for (const assetMapper of assetMappers.data?.items || []) {
      for (const mapping of assetMapper.mappings?.items || []) {
        if (mapping.destination?.topic) {
          topicSet.add(mapping.destination.topic)
        }
      }
    }

    for (const topic of topicSet) {
      nodes.set(`topic-${topic}`, {
        id: `topic-${topic}`,
        type: 'TOPIC',
        label: topic,
      })
    }

    // Create nodes for TOPIC FILTERs
    for (const filter of topicFilters.data?.items || []) {
      nodes.set(`filter-${filter.topicFilter}`, {
        id: `filter-${filter.topicFilter}`,
        type: 'TOPIC_FILTER',
        label: filter.topicFilter,
      })
    }

    // Create edges for NORTHBOUND mappings (TAG → TOPIC)
    for (const mapping of northMappings.data?.items || []) {
      edges.push({
        from: `tag-${mapping.tagName}`,
        to: `topic-${mapping.topic}`,
        type: 'NORTHBOUND',
        metadata: { tagName: mapping.tagName, topic: mapping.topic },
      })
    }

    // Create edges for SOUTHBOUND mappings (TOPIC FILTER → TAG)
    for (const mapping of southMappings.data?.items || []) {
      edges.push({
        from: `filter-${mapping.topicFilter}`,
        to: `tag-${mapping.tagName}`,
        type: 'SOUTHBOUND',
        metadata: { tagName: mapping.tagName, topicFilter: mapping.topicFilter },
      })
    }

    // Create edges for BRIDGE subscriptions (TOPIC FILTER → TOPIC)
    for (const [sourceFilter, targetTopic] of bridgeSubscriptions.mappings || []) {
      edges.push({
        from: `filter-${sourceFilter}`,
        to: `topic-${targetTopic}`,
        type: 'BRIDGE',
      })
    }

    // Create edges for COMBINERS (TAG/FILTER → TOPIC)
    for (const combiner of combiners.data?.items || []) {
      for (const mapping of combiner.mappings?.items || []) {
        const destinationTopic = mapping.destination?.topic
        if (!destinationTopic) continue

        // Edges from tags
        for (const tagName of mapping.sources?.tags || []) {
          edges.push({
            from: `tag-${tagName}`,
            to: `topic-${destinationTopic}`,
            type: 'COMBINER',
            metadata: { combinerId: combiner.id, combinerName: combiner.name },
          })
        }

        // Edges from topic filters
        for (const filterName of mapping.sources?.topicFilters || []) {
          edges.push({
            from: `filter-${filterName}`,
            to: `topic-${destinationTopic}`,
            type: 'COMBINER',
            metadata: { combinerId: combiner.id, combinerName: combiner.name },
          })
        }
      }
    }

    // Create edges for ASSET MAPPERS (TAG/FILTER → TOPIC)
    for (const assetMapper of assetMappers.data?.items || []) {
      for (const mapping of assetMapper.mappings?.items || []) {
        const destinationTopic = mapping.destination?.topic
        if (!destinationTopic) continue

        // Edges from tags
        for (const tagName of mapping.sources?.tags || []) {
          edges.push({
            from: `tag-${tagName}`,
            to: `topic-${destinationTopic}`,
            type: 'ASSET_MAPPER',
            metadata: { assetMapperId: assetMapper.id, assetMapperName: assetMapper.name },
          })
        }

        // Edges from topic filters
        for (const filterName of mapping.sources?.topicFilters || []) {
          edges.push({
            from: `filter-${filterName}`,
            to: `topic-${destinationTopic}`,
            type: 'ASSET_MAPPER',
            metadata: { assetMapperId: assetMapper.id, assetMapperName: assetMapper.name },
          })
        }
      }
    }

    return { nodes, edges }
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

  /**
   * Trace data flow from a starting node
   * Uses BFS to find all paths in the specified direction
   * BIDIRECTIONAL traces both upstream and downstream and combines results
   */
  const trace = (startNodeId: string, direction: TraceDirection, maxHops = 10): TraceResult | null => {
    const startNode = graph.nodes.get(startNodeId)
    if (!startNode) return null

    // For bidirectional, trace both directions and combine
    if (direction === 'BIDIRECTIONAL') {
      const upstreamResult = traceSingleDirection(startNodeId, 'UPSTREAM', maxHops)
      const downstreamResult = traceSingleDirection(startNodeId, 'DOWNSTREAM', maxHops)

      if (!upstreamResult && !downstreamResult) {
        return {
          path: [{ node: startNode, hopNumber: 0 }],
          direction: 'BIDIRECTIONAL',
          hasCycles: false,
          startNode,
          endNodes: [],
        }
      }

      // Combine paths: upstream (reversed) + start node + downstream
      const upstreamPath = upstreamResult?.path.slice().reverse() || []
      const downstreamPath = downstreamResult?.path.slice(1) || [] // Skip start node duplicate

      const combinedPath = [
        ...upstreamPath,
        ...(downstreamResult?.path || [{ node: startNode, hopNumber: 0 }]),
        ...downstreamPath,
      ]

      // Renumber hops
      const renumberedPath = combinedPath.map((hop, index) => ({
        ...hop,
        hopNumber: index,
      }))

      const allEndNodes = [...(upstreamResult?.endNodes || []), ...(downstreamResult?.endNodes || [])].filter(
        (node) => node.id !== startNodeId
      )

      return {
        path: renumberedPath,
        direction: 'BIDIRECTIONAL',
        hasCycles: upstreamResult?.hasCycles || false || downstreamResult?.hasCycles || false,
        startNode,
        endNodes: allEndNodes,
      }
    }

    // Single direction trace
    return traceSingleDirection(startNodeId, direction, maxHops)
  }

  /**
   * Internal helper to trace in a single direction
   */
  const traceSingleDirection = (
    startNodeId: string,
    direction: 'UPSTREAM' | 'DOWNSTREAM',
    maxHops: number
  ): TraceResult | null => {
    const startNode = graph.nodes.get(startNodeId)
    if (!startNode) return null

    const visited = new Set<string>()
    const allPaths: TraceHop[][] = []
    let hasCycles = false

    // BFS queue: [nodeId, path, hopNumber]
    const queue: [string, TraceHop[], number][] = [[startNodeId, [{ node: startNode, hopNumber: 0 }], 0]]

    while (queue.length > 0) {
      const [currentNodeId, currentPath, hopNumber] = queue.shift()!

      if (hopNumber >= maxHops) {
        continue // Prevent infinite loops
      }

      // Find adjacent nodes based on direction
      const adjacentEdges =
        direction === 'DOWNSTREAM'
          ? graph.edges.filter((e) => e.from === currentNodeId)
          : graph.edges.filter((e) => e.to === currentNodeId)

      if (adjacentEdges.length === 0) {
        // Reached end of path
        allPaths.push(currentPath)
        continue
      }

      for (const edge of adjacentEdges) {
        const nextNodeId = direction === 'DOWNSTREAM' ? edge.to : edge.from
        const nextNode = graph.nodes.get(nextNodeId)

        if (!nextNode) continue

        // Check for cycles
        if (visited.has(nextNodeId)) {
          hasCycles = true
          continue
        }

        const newPath = [
          ...currentPath,
          {
            node: nextNode,
            edge: direction === 'DOWNSTREAM' ? edge : { ...edge, from: edge.to, to: edge.from },
            hopNumber: hopNumber + 1,
          },
        ]

        queue.push([nextNodeId, newPath, hopNumber + 1])
        visited.add(currentNodeId) // Mark as visited after exploring
      }
    }

    // If no paths found, return just the start node
    if (allPaths.length === 0) {
      allPaths.push([{ node: startNode, hopNumber: 0 }])
    }

    // For now, return the first (shortest) path
    // TODO: Support multiple paths in UI
    const mainPath = allPaths[0]
    const endNodes = allPaths.map((path) => path.at(-1)!.node)

    return {
      path: mainPath,
      direction,
      hasCycles,
      startNode,
      endNodes,
    }
  }

  /**
   * Get all available nodes for tracing
   */
  const getAvailableNodes = (): GraphNode[] => {
    return Array.from(graph.nodes.values())
  }

  return {
    trace,
    getAvailableNodes,
    isLoading,
    graph,
  }
}
