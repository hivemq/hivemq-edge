/**
 * Ghost Node Factory
 *
 * Factory functions to create ghost nodes and edges for preview during wizard.
 * Ghost nodes show users what will be created before they complete the wizard.
 * Supports multi-node previews with proper positioning and connections.
 */

import type { Node, XYPosition } from '@xyflow/react'
import { MarkerType, Position } from '@xyflow/react'

import type { GhostNode, GhostEdge } from '../types'
import { EntityType } from '../types'
import { EdgeTypes, IdStubs, NodeTypes } from '@/modules/Workspace/types'

/**
 * Positioning constants (from nodes-utils.ts)
 */
const POS_NODE_INC: XYPosition = { x: 325, y: 400 }
const MAX_ADAPTERS = 10
const GLUE_SEPARATOR = 200

/**
 * Base ghost node properties
 */
const GHOST_BASE = {
  draggable: false,
  selectable: false,
  connectable: false,
}

/**
 * Enhanced ghost node styling with glowing effect
 */
export const GHOST_STYLE_ENHANCED = {
  opacity: 0.75,
  border: '3px dashed #4299E1',
  backgroundColor: '#EBF8FF',
  boxShadow: '0 0 0 4px rgba(66, 153, 225, 0.4), 0 0 20px rgba(66, 153, 225, 0.6)',
  pointerEvents: 'none' as const,
  transition: 'all 0.3s ease',
}

/**
 * Ghost edge styling
 */
export const GHOST_EDGE_STYLE = {
  stroke: '#4299E1',
  strokeWidth: 2,
  strokeDasharray: '5,5',
  opacity: 0.6,
}

/**
 * Legacy ghost node styling (for backward compatibility)
 */
export const GHOST_STYLE = {
  opacity: 0.6,
  border: '2px dashed #4299E1',
  backgroundColor: '#EBF8FF',
  pointerEvents: 'none' as const,
}

/**
 * Ghost node group (multi-node preview)
 */
export interface GhostNodeGroup {
  nodes: GhostNode[]
  edges: GhostEdge[]
}

/**
 * Create a ghost adapter node
 */
export const createGhostAdapter = (id: string, label: string = 'New Adapter'): GhostNode => {
  return {
    ...GHOST_BASE,
    id: `ghost-${id}`,
    type: 'ADAPTER_NODE',
    position: { x: 200, y: 200 },
    data: {
      isGhost: true,
      label,
      id: `ghost-${id}`,
      status: {
        connection: 'STATELESS',
        runtime: 'STOPPED',
      },
    },
    style: GHOST_STYLE,
  }
}

/**
 * Create a ghost bridge node
 */
export const createGhostBridge = (id: string, label: string = 'New Bridge'): GhostNode => {
  return {
    ...GHOST_BASE,
    id: `ghost-${id}`,
    type: 'BRIDGE_NODE',
    position: { x: 200, y: 200 },
    data: {
      isGhost: true,
      label,
      id: `ghost-${id}`,
      status: {
        connection: 'STATELESS',
        runtime: 'STOPPED',
      },
    },
    style: GHOST_STYLE,
  }
}

/**
 * Create a ghost combiner node
 */
export const createGhostCombiner = (id: string, label: string = 'New Combiner'): GhostNode => {
  return {
    ...GHOST_BASE,
    id: `ghost-${id}`,
    type: 'COMBINER_NODE',
    position: { x: 200, y: 200 },
    data: {
      isGhost: true,
      label,
      id: `ghost-${id}`,
      status: {
        connection: 'STATELESS',
        runtime: 'STOPPED',
      },
    },
    style: GHOST_STYLE,
  }
}

/**
 * Create a ghost asset mapper (Pulse) node
 */
export const createGhostAssetMapper = (id: string, label: string = 'New Asset Mapper'): GhostNode => {
  return {
    ...GHOST_BASE,
    id: `ghost-${id}`,
    type: 'PULSE_NODE',
    position: { x: 200, y: 200 },
    data: {
      isGhost: true,
      label,
      id: `ghost-${id}`,
      status: {
        connection: 'STATELESS',
        runtime: 'STOPPED',
      },
    },
    style: GHOST_STYLE,
  }
}

/**
 * Create a ghost group node
 */
export const createGhostGroup = (id: string, label: string = 'New Group'): GhostNode => {
  return {
    ...GHOST_BASE,
    id: `ghost-${id}`,
    type: 'CLUSTER_NODE',
    position: { x: 200, y: 200 },
    data: {
      isGhost: true,
      label,
      id: `ghost-${id}`,
      childrenNodeIds: [],
    },
    style: {
      ...GHOST_STYLE,
      width: 300,
      height: 200,
    },
  }
}

/**
 * Factory function to create ghost node based on entity type
 */
export const createGhostNodeForType = (entityType: EntityType, id: string = 'preview'): GhostNode | null => {
  switch (entityType) {
    case EntityType.ADAPTER:
      return createGhostAdapter(id)
    case EntityType.BRIDGE:
      return createGhostBridge(id)
    case EntityType.COMBINER:
      return createGhostCombiner(id)
    case EntityType.ASSET_MAPPER:
      return createGhostAssetMapper(id)
    case EntityType.GROUP:
      return createGhostGroup(id)
    default:
      return null
  }
}

/**
 * Check if a node is a ghost node
 */
export const isGhostNode = (node: { data?: { isGhost?: boolean } }): boolean => {
  return node.data?.isGhost === true
}

/**
 * Get all ghost node IDs from a list of nodes
 */
export const getGhostNodeIds = (nodes: Array<{ id: string; data?: { isGhost?: boolean } }>): string[] => {
  return nodes.filter(isGhostNode).map((node) => node.id)
}

/**
 * Remove ghost nodes from a list of nodes
 */
export const removeGhostNodes = <T extends { data?: { isGhost?: boolean } }>(nodes: T[]): T[] => {
  return nodes.filter((node) => !isGhostNode(node))
}

/**
 * Calculate ghost adapter position using the same algorithm as real nodes
 * This ensures smooth transition from ghost → real with no position jump
 */
export const calculateGhostAdapterPosition = (
  nbAdapters: number,
  edgeNodePos: XYPosition
): { adapterPos: XYPosition; devicePos: XYPosition } => {
  // Ghost should be positioned one slot to the right of the last adapter
  const posX = (nbAdapters + 1) % MAX_ADAPTERS
  const posY = Math.floor((nbAdapters + 1) / MAX_ADAPTERS) + 1
  const deltaX = Math.floor((Math.min(MAX_ADAPTERS, nbAdapters + 2) - 1) / 2)

  const adapterPos = {
    x: edgeNodePos.x + POS_NODE_INC.x * (posX - deltaX),
    y: edgeNodePos.y - POS_NODE_INC.y * posY * 1.5,
  }

  const devicePos = {
    x: adapterPos.x,
    y: adapterPos.y - GLUE_SEPARATOR, // Device ABOVE adapter
  }

  return { adapterPos, devicePos }
}

/**
 * Create a complete ghost adapter group (ADAPTER + DEVICE + connections)
 * This provides a full preview of what will be created
 */
export const createGhostAdapterGroup = (
  id: string,
  nbAdapters: number,
  edgeNode: Node,
  label: string = 'New Adapter'
): GhostNodeGroup => {
  const { adapterPos, devicePos } = calculateGhostAdapterPosition(nbAdapters, edgeNode.position)

  // Create ADAPTER ghost node
  const adapterNode: GhostNode = {
    ...GHOST_BASE,
    id: `ghost-adapter-${id}`,
    type: NodeTypes.ADAPTER_NODE,
    position: adapterPos,
    sourcePosition: Position.Bottom, // Connects down to EDGE
    targetPosition: Position.Top, // Receives from DEVICE above
    data: {
      isGhost: true,
      label,
      id: `ghost-adapter-${id}`,
      status: {
        connection: 'STATELESS',
        runtime: 'STOPPED',
      },
    },
    style: GHOST_STYLE_ENHANCED,
  }

  // Create DEVICE ghost node
  const deviceNode: GhostNode = {
    ...GHOST_BASE,
    id: `ghost-device-${id}`,
    type: NodeTypes.DEVICE_NODE,
    position: devicePos,
    sourcePosition: Position.Bottom, // Device connects from bottom to ADAPTER
    targetPosition: Position.Top, // Just in case, though DEVICE doesn't receive connections
    data: {
      isGhost: true,
      label: `${label} Device`,
    },
    style: GHOST_STYLE_ENHANCED,
  }

  // Create edge from ADAPTER to EDGE node
  const edgeToEdge: GhostEdge = {
    id: `ghost-edge-adapter-to-edge-${id}`,
    source: `ghost-adapter-${id}`,
    target: IdStubs.EDGE_NODE,
    targetHandle: 'Top', // EDGE node has this handle
    focusable: false,
    type: EdgeTypes.DYNAMIC_EDGE,
    animated: true,
    style: GHOST_EDGE_STYLE,
    markerEnd: {
      type: MarkerType.ArrowClosed,
      width: 20,
      height: 20,
      color: '#4299E1',
    },
    data: {
      isGhost: true,
    },
  }

  // Create edge from DEVICE to ADAPTER
  const edgeToDevice: GhostEdge = {
    id: `ghost-edge-device-to-adapter-${id}`,
    source: `ghost-device-${id}`,
    target: `ghost-adapter-${id}`,
    // No handles specified - ghost nodes use default positions
    focusable: false,
    type: EdgeTypes.DYNAMIC_EDGE,
    animated: true,
    style: GHOST_EDGE_STYLE,
    markerEnd: {
      type: MarkerType.ArrowClosed,
      width: 20,
      height: 20,
      color: '#4299E1',
    },
    data: {
      isGhost: true,
    },
  }

  return {
    nodes: [adapterNode, deviceNode],
    edges: [edgeToEdge, edgeToDevice],
  }
}

/**
 * Helper to check if an edge is a ghost edge
 */
export const isGhostEdge = (edge: { id?: string; data?: { isGhost?: boolean } }): boolean => {
  return edge.id?.startsWith('ghost-') || edge.data?.isGhost === true
}

/**
 * Remove ghost edges from a list of edges
 */
export const removeGhostEdges = <T extends { id?: string; data?: { isGhost?: boolean } }>(edges: T[]): T[] => {
  return edges.filter((edge) => !isGhostEdge(edge))
}
