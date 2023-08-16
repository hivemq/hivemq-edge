import { useEffect } from 'react'
import { Edge, Node, useEdgesState, useNodesState, XYPosition, MarkerType } from 'reactflow'

import { Adapter, Bridge } from '@/api/__generated__'
import { useListProtocolAdapters } from '@/api/hooks/useProtocolAdapters/useListProtocolAdapters.tsx'
import { useListBridges } from '@/api/hooks/useGetBridges/useListBridges.tsx'

import { IdStubs, NodeTypes } from '../types.ts'

const POS_EDGE: XYPosition = { x: 300, y: 300 }
const POS_NODE_INC: XYPosition = { x: 150 + 8, y: 100 }

export const createEdgeNode = (label: string, positionStorage?: Record<string, XYPosition>) => {
  const nodeEdge: Node<unknown, NodeTypes.EDGE_NODE> = {
    id: IdStubs.EDGE_NODE,
    type: NodeTypes.EDGE_NODE,
    data: { label: label },
    position: positionStorage?.[IdStubs.EDGE_NODE] ?? POS_EDGE,
  }
  return nodeEdge
}

export const createBridgeNode = (bridge: Bridge, nbBridge: number, positionStorage?: Record<string, XYPosition>) => {
  const idBridge = `${IdStubs.BRIDGE_NODE}#${bridge.id}`

  const nodeBridge: Node<Bridge, NodeTypes.BRIDGE_NODE> = {
    id: idBridge,
    type: NodeTypes.BRIDGE_NODE,
    // @ts-ignore To force a label on the default node
    data: { ...bridge, label: bridge.id },
    position: positionStorage?.[idBridge] ?? {
      x: POS_EDGE.x + POS_NODE_INC.x * (nbBridge - 1),
      y: POS_EDGE.y + POS_NODE_INC.y,
    },
  }

  const edgeConnector: Edge = {
    id: `${IdStubs.CONNECTOR}-${IdStubs.EDGE_NODE}-${idBridge}`,
    source: IdStubs.EDGE_NODE,
    target: idBridge,
    markerStart: {
      type: MarkerType.ArrowClosed,
      width: 20,
      height: 20,
    },
    animated: false,
    // label: bridge.host,
    // type: 'step',
  }

  // TODO[NVL] Add separate node for the host ?
  return { nodeBridge, edgeConnector }
}

export const createAdapterNode = (
  adapter: Adapter,
  nbAdapter: number,
  positionStorage?: Record<string, XYPosition>
) => {
  const idAdapter = `${IdStubs.ADAPTER_NODE}#${adapter.id}`

  const nodeAdapter: Node<Adapter, NodeTypes.ADAPTER_NODE> = {
    id: idAdapter,
    type: NodeTypes.ADAPTER_NODE,
    // @ts-ignore To force a label on the default node
    data: { ...adapter, label: adapter.id },
    // position: positionStorage?.[idAdapter] ?? { x: POS_EDGE.x + 200 * (nbAdapter - 1), y: POS_EDGE.y - 250 },
    position: positionStorage?.[idAdapter] ?? {
      x: POS_EDGE.x + POS_NODE_INC.x * (nbAdapter - 1),
      y: POS_EDGE.y - POS_NODE_INC.y,
    },
  }

  const edgeConnector: Edge = {
    id: `${IdStubs.CONNECTOR}-${IdStubs.EDGE_NODE}-${idAdapter}`,
    target: IdStubs.EDGE_NODE,
    source: idAdapter,
    markerEnd: {
      type: MarkerType.ArrowClosed,
      width: 20,
      height: 20,
      // color: '#000',
    },
    animated: false,
    // label: bridge.host,
    // type: 'step',
  }

  return { nodeAdapter, edgeConnector }
}

const useGetFlowElements = () => {
  const { data: bridges } = useListBridges()
  const { data: adapters } = useListProtocolAdapters()

  const [nodes, setNodes, onNodesChange] = useNodesState<Bridge | Adapter>([])
  const [edges, setEdges, onEdgesChange] = useEdgesState([])

  useEffect(() => {
    if (!bridges) return
    if (!adapters) return

    const nodes: Node[] = []
    const edges: Edge[] = []

    const nodeEdge = createEdgeNode('Your Edge')

    bridges.forEach((bridge, incBridgeNb) => {
      const { nodeBridge, edgeConnector } = createBridgeNode(bridge, incBridgeNb)
      nodes.push(nodeBridge)
      edges.push(edgeConnector)
    })

    adapters.forEach((adapter, incAdapterNb) => {
      const { nodeAdapter, edgeConnector } = createAdapterNode(adapter, incAdapterNb)
      nodes.push(nodeAdapter)
      edges.push(edgeConnector)
    })

    setNodes([nodeEdge, ...nodes])
    setEdges([...edges])
  }, [bridges, adapters, setNodes, setEdges])

  return { nodes, edges, onNodesChange, onEdgesChange }
}

export default useGetFlowElements
