import type { FC } from 'react'
import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import type { Edge, Node } from '@xyflow/react'
import { Background, Controls, MiniMap, ReactFlow, useEdgesState, useNodesState } from '@xyflow/react'
import { Box } from '@chakra-ui/react'
import '@xyflow/react/dist/style.css'

import ErrorMessage from '@/components/ErrorMessage.tsx'
import LoaderSpinner from '@/components/Chakra/LoaderSpinner.tsx'
import ChartWrapper from '@/modules/DomainOntology/components/parts/ChartWrapper.tsx'
import NetworkGraphNode from '@/modules/DomainOntology/components/network-graph/NetworkGraphNode.tsx'
import NetworkGraphEdge from '@/modules/DomainOntology/components/network-graph/NetworkGraphEdge.tsx'
import NetworkGraphDetailsPanel from '@/modules/DomainOntology/components/network-graph/NetworkGraphDetailsPanel.tsx'
import NetworkGraphDetailsPanelContainer from '@/modules/DomainOntology/components/network-graph/NetworkGraphDetailsPanelContainer.tsx'
import {
  useGetNetworkGraphData,
  type NetworkGraphNode as NetworkGraphNodeType,
  type NetworkGraphEdge as NetworkGraphEdgeType,
} from '@/modules/DomainOntology/hooks/useGetNetworkGraphData.ts'

// Define custom node and edge types
const nodeTypes = {
  networkGraphNode: NetworkGraphNode,
}

const edgeTypes = {
  networkGraphEdge: NetworkGraphEdge,
}

// Force-directed layout algorithm
const applyForceDirectedLayout = (
  nodes: NetworkGraphNodeType[],
  edges: NetworkGraphEdgeType[]
): NetworkGraphNodeType[] => {
  if (nodes.length === 0) return []

  // Constants for force simulation
  const REPULSION_STRENGTH = 15000 // Repulsion between all nodes (higher = more spread)
  const ATTRACTION_STRENGTH = 0.1 // Attraction along edges (higher = tighter clusters)
  const ITERATIONS = 200 // Number of simulation iterations
  const DAMPING = 0.85 // Velocity damping factor
  const CENTER_X = 400
  const CENTER_Y = 300

  // Initialize positions randomly in a circle
  const positions = nodes.map((node, index) => {
    const angle = (index / nodes.length) * 2 * Math.PI
    const radius = 200
    return {
      id: node.id,
      x: CENTER_X + radius * Math.cos(angle),
      y: CENTER_Y + radius * Math.sin(angle),
      vx: 0,
      vy: 0,
    }
  })

  // Create position lookup
  const posMap = new Map(positions.map((p) => [p.id, p]))

  // Run force simulation
  for (let iter = 0; iter < ITERATIONS; iter++) {
    // Calculate repulsive forces between all nodes
    for (let i = 0; i < positions.length; i++) {
      for (let j = i + 1; j < positions.length; j++) {
        const p1 = positions[i]
        const p2 = positions[j]

        const dx = p2.x - p1.x
        const dy = p2.y - p1.y
        const distSq = dx * dx + dy * dy + 1 // +1 to avoid division by zero
        const dist = Math.sqrt(distSq)

        // Repulsion force (inversely proportional to distance)
        const force = REPULSION_STRENGTH / distSq
        const fx = (dx / dist) * force
        const fy = (dy / dist) * force

        // Apply repulsion (push apart)
        p1.vx -= fx
        p1.vy -= fy
        p2.vx += fx
        p2.vy += fy
      }
    }

    // Calculate attractive forces along edges
    // Instead of pulling together, we push target away from source (directional flow)
    for (const edge of edges) {
      const source = posMap.get(edge.source)
      const target = posMap.get(edge.target)

      if (source && target) {
        const dx = target.x - source.x
        const dy = target.y - source.y
        const dist = Math.sqrt(dx * dx + dy * dy)

        // Push force: target should be pushed away from source
        // This creates a directional flow visualization
        const idealDistance = 250 // Desired distance between connected nodes
        const displacement = idealDistance - dist

        if (displacement > 0) {
          // Too close - push apart
          const force = displacement * ATTRACTION_STRENGTH * 2
          const fx = (dx / dist) * force
          const fy = (dy / dist) * force

          // Push target away from source
          target.vx += fx
          target.vy += fy
          // Push source away from target (less force)
          source.vx -= fx * 0.3
          source.vy -= fy * 0.3
        } else {
          // Too far - pull together slightly
          const force = Math.abs(displacement) * ATTRACTION_STRENGTH * 0.5
          const fx = (dx / dist) * force
          const fy = (dy / dist) * force

          target.vx -= fx
          target.vy -= fy
          source.vx += fx * 0.5
          source.vy += fy * 0.5
        }
      }
    }

    // Apply centering force (weak pull to center)
    for (const pos of positions) {
      const dx = CENTER_X - pos.x
      const dy = CENTER_Y - pos.y
      pos.vx += dx * 0.005 // Increased from 0.001 for stronger centering
      pos.vy += dy * 0.005
    }

    // Update positions with damping
    for (const pos of positions) {
      pos.x += pos.vx
      pos.y += pos.vy
      pos.vx *= DAMPING
      pos.vy *= DAMPING
    }
  }

  // Calculate statistics for debugging
  const distances: number[] = []
  for (let i = 0; i < positions.length; i++) {
    for (let j = i + 1; j < positions.length; j++) {
      const dx = positions[j].x - positions[i].x
      const dy = positions[j].y - positions[i].y
      distances.push(Math.sqrt(dx * dx + dy * dy))
    }
  }

  const avgDistance = distances.length > 0 ? distances.reduce((a, b) => a + b, 0) / distances.length : 0
  const minDistance = distances.length > 0 ? Math.min(...distances) : 0
  const maxDistance = distances.length > 0 ? Math.max(...distances) : 0

  // Log layout results ONCE per calculation
  console.log('🎯 Force-Directed Layout Complete', {
    nodeCount: nodes.length,
    edgeCount: edges.length,
    nodeTypes: nodes.reduce(
      (acc, n) => {
        acc[n.data.type] = (acc[n.data.type] || 0) + 1
        return acc
      },
      {} as Record<string, number>
    ),
    edgeTypes: edges.reduce(
      (acc, e) => {
        const type = e.data?.transformationType || 'UNKNOWN'
        acc[type] = (acc[type] || 0) + 1
        return acc
      },
      {} as Record<string, number>
    ),
    distances: {
      avg: Math.round(avgDistance),
      min: Math.round(minDistance),
      max: Math.round(maxDistance),
    },
    boundingBox: {
      minX: Math.round(Math.min(...positions.map((p) => p.x))),
      maxX: Math.round(Math.max(...positions.map((p) => p.x))),
      minY: Math.round(Math.min(...positions.map((p) => p.y))),
      maxY: Math.round(Math.max(...positions.map((p) => p.y))),
    },
  })

  // Log sample of node positions and their connections
  console.log(
    '📋 Sample node positions:',
    positions.slice(0).map((p) => ({
      id: p.id,
      x: Math.round(p.x),
      y: Math.round(p.y),
    }))
  )

  // Log edge connections
  console.log(
    '🔗 Edge connections:',
    edges.slice(0).map((e) => ({
      from: e.source,
      to: e.target,
      type: e.data?.transformationType,
    }))
  )

  // Apply calculated positions to nodes
  return nodes.map((node) => {
    const pos = posMap.get(node.id)
    return {
      ...node,
      position: {
        x: pos?.x ?? 0,
        y: pos?.y ?? 0,
      },
    }
  })
}

const NetworkGraphView: FC = () => {
  const { t } = useTranslation()
  const { nodes: initialNodes, edges: initialEdges, isError, isLoading } = useGetNetworkGraphData()

  // Initialize with empty nodes/edges with proper React Flow typing
  const [nodes, setNodes, onNodesChange] = useNodesState<Node>([])
  const [edges, setEdges, onEdgesChange] = useEdgesState<Edge>([])

  // Node selection state for details panel
  const [selectedNodeId, setSelectedNodeId] = useState<string | null>(null)
  const selectedNode = useMemo(() => nodes.find((n) => n.id === selectedNodeId) || null, [nodes, selectedNodeId])

  // Track previous node count to detect when data changes
  const prevNodeCountRef = useRef(0)

  // Update nodes/edges when data count changes (async data loading)
  useEffect(() => {
    const currentCount = initialNodes.length
    if (currentCount > 0 && currentCount !== prevNodeCountRef.current) {
      console.log('📥 Data loaded, updating graph', {
        from: prevNodeCountRef.current,
        to: currentCount,
      })
      // Calculate layout with new data
      const newLayoutedNodes = applyForceDirectedLayout(initialNodes, initialEdges)
      setNodes(newLayoutedNodes)
      setEdges(initialEdges)
      prevNodeCountRef.current = currentCount
    }
  }, [initialNodes.length, initialNodes, initialEdges, setNodes, setEdges])

  // Node click handler - opens details panel
  const onNodeClick = useCallback((_event: React.MouseEvent, node: { id: string }) => {
    console.log('Node clicked:', node.id)
    setSelectedNodeId(node.id)
  }, [])

  // Clear selection handler
  const clearSelection = useCallback(() => {
    setSelectedNodeId(null)
  }, [])

  if (isLoading) return <LoaderSpinner />
  if (isError) return <ErrorMessage type={t('ontology.error.loading')} />

  if (!nodes.length) {
    return (
      <ChartWrapper data-testid="edge-panel-network-graph">
        <Box p={8} textAlign="center" color="gray.500">
          {t('ontology.error.noDataLoaded')}
        </Box>
      </ChartWrapper>
    )
  }

  return (
    <ChartWrapper data-testid="edge-panel-network-graph">
      <Box position="relative" h="600px" w="100%">
        <ReactFlow
          nodes={nodes}
          edges={edges}
          onNodesChange={onNodesChange}
          onEdgesChange={onEdgesChange}
          onNodeClick={onNodeClick}
          nodeTypes={nodeTypes}
          edgeTypes={edgeTypes}
          fitView
          minZoom={0.1}
          maxZoom={2}
          defaultEdgeOptions={{
            animated: false,
          }}
        >
          <Background />
          <Controls />
          <MiniMap
            nodeStrokeWidth={3}
            zoomable
            pannable
            style={{
              backgroundColor: 'var(--chakra-colors-gray-50)',
            }}
          />
        </ReactFlow>

        {/* Details Panel - Container handles layout, Panel handles content */}
        <NetworkGraphDetailsPanelContainer isOpen={!!selectedNode}>
          {selectedNode && (
            <NetworkGraphDetailsPanel
              node={selectedNode as NetworkGraphNodeType}
              edges={edges as NetworkGraphEdgeType[]}
              onClose={clearSelection}
              onNavigateToConfig={(nodeId) => console.log('Navigate to config:', nodeId)}
              onFilterByNode={(nodeId) => console.log('Filter by node:', nodeId)}
            />
          )}
        </NetworkGraphDetailsPanelContainer>
      </Box>
    </ChartWrapper>
  )
}

export default NetworkGraphView
