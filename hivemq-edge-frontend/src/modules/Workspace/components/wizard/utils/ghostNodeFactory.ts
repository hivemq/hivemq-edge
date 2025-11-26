import type { Node, XYPosition, Edge } from '@xyflow/react'
import { MarkerType, Position } from '@xyflow/react'

import { Status } from '@/api/__generated__'
import type { GhostNode, GhostEdge } from '../types'
import { EntityType } from '../types'
import { EdgeTypes, IdStubs, NodeTypes } from '@/modules/Workspace/types'
import { getAutoIncludedNodes } from './groupConstraints'

import i18n from '@/config/i18n.config.ts'

/**
 * Positioning constants (from nodes-utils.ts)
 */
const POS_NODE_INC: XYPosition = { x: 325, y: 400 }
const MAX_ADAPTERS = 10
const GLUE_SEPARATOR = 200

/**
 * GHOST-related colors
 * TODO[NVL] These must be integrated in the theme
 */
export const GHOST_COLOR_BACKGROUND = '#EBF8FF'
export const GHOST_COLOR_EDGE = '#4299E1'
export const GHOST_SUCCESS_SHADOW = '0 0 0 4px rgba(72, 187, 120, 0.6), 0 0 20px rgba(72, 187, 120, 0.4)'
export const GHOST_SUCCESS_TRANSITION = 'box-shadow 0.3s ease-in'
export const GHOST_SUCCESS_DIMMED_TRANSITION = 'box-shadow 0.5s ease-out'

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
  border: `3px dashed ${GHOST_COLOR_EDGE}`,
  backgroundColor: GHOST_COLOR_BACKGROUND,
  boxShadow: '0 0 0 4px rgba(66, 153, 225, 0.4), 0 0 20px rgba(66, 153, 225, 0.6)',
  pointerEvents: 'none' as const,
  transition: 'all 0.3s ease',
}

/**
 * Selectable ghost node styling - allows clicking to see edge highlighting
 */
export const GHOST_STYLE_SELECTABLE = {
  ...GHOST_STYLE_ENHANCED,
  pointerEvents: 'all' as const, // Allow interaction
  cursor: 'pointer' as const,
}

/**
 * Ghost edge styling
 */
export const GHOST_EDGE_STYLE = {
  stroke: GHOST_COLOR_EDGE,
  strokeWidth: 2,
  strokeDasharray: '5,5',
  opacity: 0.6,
}

/**
 * Legacy ghost node styling (for backward compatibility)
 */
export const GHOST_STYLE = {
  opacity: 0.6,
  border: `2px dashed ${GHOST_COLOR_EDGE}`,
  backgroundColor: GHOST_COLOR_BACKGROUND,
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
export const createGhostAdapter = (id: string, label: string = i18n.t('workspace.ghost.adapter')): GhostNode => {
  return {
    ...GHOST_BASE,
    id: `ghost-${id}`,
    type: NodeTypes.ADAPTER_NODE,
    position: { x: 200, y: 200 },
    data: {
      isGhost: true,
      label,
      id: `ghost-${id}`,
      status: {
        connection: Status.connection.STATELESS,
        runtime: Status.runtime.STOPPED,
      },
    },
    style: GHOST_STYLE,
  }
}

/**
 * Create a ghost bridge node
 */
export const createGhostBridge = (id: string, label: string = i18n.t('workspace.ghost.bridge')): GhostNode => {
  return {
    ...GHOST_BASE,
    id: `ghost-${id}`,
    type: NodeTypes.BRIDGE_NODE,
    position: { x: 200, y: 200 },
    data: {
      isGhost: true,
      label,
      id: `ghost-${id}`,
      status: {
        connection: Status.connection.STATELESS,
        runtime: Status.runtime.STOPPED,
      },
    },
    style: GHOST_STYLE,
  }
}

/**
 * Create a ghost combiner node positioned near EDGE node
 * Uses a UUID for the ID to satisfy validation requirements
 */
export const createGhostCombiner = (
  id: string,
  edgeNode: Node,
  label: string = i18n.t('workspace.ghost.combiner')
): GhostNode => {
  // Position to the right of EDGE node
  const pos = {
    x: edgeNode.position.x + 400,
    y: edgeNode.position.y,
  }

  // Generate a UUID for the combiner ID to satisfy validation
  const combinerId = crypto.randomUUID()

  return {
    ...GHOST_BASE,
    id: `ghost-combiner-${id}`, // Node ID can be descriptive
    type: NodeTypes.COMBINER_NODE,
    position: pos,
    selectable: true, // Override base - allow selection
    draggable: false, // Keep non-draggable
    connectable: false, // Keep non-connectable
    data: {
      isGhost: true,
      label, // Required by GhostNode type
      // Required Combiner fields
      id: combinerId, // Use UUID for combiner data ID
      name: label,
      // sources: EntityReferenceList with empty items
      sources: {
        items: [],
      },
      // mappings: DataCombiningList with empty items
      mappings: {
        items: [],
      },
      // Status for node display
      status: {
        connection: Status.connection.STATELESS,
        runtime: Status.runtime.STOPPED,
      },
    },
    style: GHOST_STYLE_SELECTABLE, // Use selectable style
  }
}

/**
 * Create a ghost asset mapper (Pulse) node
 * Asset Mapper IS a Combiner, just with Pulse Agent auto-included in sources
 * Reuse createGhostCombiner to avoid duplication
 */
export const createGhostAssetMapper = (
  id: string,
  edgeNode: Node,
  label: string = i18n.t('workspace.ghost.mapper')
): GhostNode => {
  // Asset Mapper uses exact same structure as Combiner
  return createGhostCombiner(id, edgeNode, label)
}

export const createGhostCombinerGroup = (id: string, edgeNode: Node, entityType: EntityType): GhostNodeGroup => {
  const ghostNode = createGhostCombiner(
    id,
    edgeNode,
    entityType === EntityType.COMBINER ? i18n.t('workspace.ghost.combiner') : i18n.t('workspace.ghost.mapper')
  )

  const edgeId = entityType === EntityType.COMBINER ? 'ghost-edge-combiner-to-edge' : 'ghost-edge-assetmapper-to-edge'

  // Create ghost edge from asset mapper to EDGE node
  const ghostEdge: GhostEdge = {
    id: edgeId,
    source: ghostNode.id,
    target: edgeNode.id,
    type: EdgeTypes.DYNAMIC_EDGE,
    animated: true,
    focusable: false,
    style: GHOST_EDGE_STYLE,
    markerEnd: {
      type: MarkerType.ArrowClosed,
      width: 20,
      height: 20,
      color: GHOST_COLOR_EDGE,
    },
    data: { isGhost: true },
  }

  return {
    nodes: [ghostNode],
    edges: [ghostEdge],
  }
}

/**
 * Get all descendants of a group node recursively
 * Used for cloning nested groups in ghost preview
 */
const getAllDescendants = (groupNode: Node, allNodes: Node[]): Node[] => {
  if (groupNode.type !== NodeTypes.CLUSTER_NODE) {
    return []
  }

  const descendants: Node[] = []
  const childIds = (groupNode.data?.childrenNodeIds || []) as string[]

  childIds.forEach((childId) => {
    const child = allNodes.find((n) => n.id === childId)
    if (!child) return

    descendants.push(child)

    // Recursively get descendants of nested groups
    if (child.type === NodeTypes.CLUSTER_NODE) {
      const nestedDescendants = getAllDescendants(child, allNodes)
      descendants.push(...nestedDescendants)
    }
  })

  return descendants
}

/**
 * Create a ghost group node with children for dynamic preview
 *
 * This function creates a ghost group that updates in real-time during selection (Step 0).
 * Key differences from other ghost nodes:
 * - Appears when FIRST node is selected
 * - Updates boundary when nodes added/removed
 * - Returns null for empty selection
 * - Children nodes are clones with parentId set
 * - Group node MUST be first in array (React Flow requirement)
 *
 * @param selectedNodes - Currently selected nodes (manually selected)
 * @param allNodes - All workspace nodes
 * @param allEdges - All workspace edges
 * @param getNodesBounds - React Flow function to calculate bounding box
 * @param getGroupBounds - Utility to add padding to group bounds
 * @returns GhostNodeGroup with group + children, or null if no selection
 */
export const createGhostGroupWithChildren = (
  selectedNodes: Node[],
  allNodes: Node[],
  allEdges: Edge[],
  getNodesBounds: (nodes: Node[]) => { x: number; y: number; width: number; height: number },
  getGroupBounds: (rect: { x: number; y: number; width: number; height: number }) => {
    x: number
    y: number
    width: number
    height: number
  }
): GhostNodeGroup | null => {
  // Handle empty selection - return null (no ghost group)
  if (selectedNodes.length === 0) {
    return null
  }

  // Calculate auto-included DEVICE/HOST nodes
  const autoIncludedNodes = getAutoIncludedNodes(selectedNodes, allNodes, allEdges)

  // All nodes that will be in the group
  const allGroupNodes = [...selectedNodes, ...autoIncludedNodes]

  // Calculate group bounds
  const rect = getNodesBounds(allGroupNodes)
  const groupRect = getGroupBounds(rect)

  // Create stable ghost group ID for selection phase
  const ghostGroupId = 'ghost-group-selection'

  // Create ghost group node (semi-transparent container)
  const ghostGroupNode: GhostNode = {
    ...GHOST_BASE,
    id: ghostGroupId,
    type: NodeTypes.CLUSTER_NODE,
    position: { x: groupRect.x, y: groupRect.y },
    style: {
      ...GHOST_STYLE_ENHANCED,
      width: groupRect.width,
      height: groupRect.height,
    },
    data: {
      isGhost: true,
      label: i18n.t('workspace.ghost.group'),
      childrenNodeIds: allGroupNodes.map((n) => n.id),
      title: i18n.t('workspace.grouping.untitled'),
      isOpen: true,
      colorScheme: 'blue',
    },
    selectable: false,
    draggable: false,
  }

  // Create ghost children (selected nodes shown within ghost boundary)
  // These are clones with parentId set and ghost styling
  // For nested groups, we also clone their descendants recursively
  const ghostChildren: GhostNode[] = []
  const processedNodes = new Set<string>()

  allGroupNodes.forEach((node) => {
    if (processedNodes.has(node.id)) return

    const label = String(node.data?.label || node.id)

    // Create ghost for the top-level node
    const ghostNode: GhostNode = {
      ...node,
      id: `ghost-child-${node.id}`,
      data: {
        ...node.data,
        isGhost: true as const,
        label,
        _originalNodeId: node.id,
      },
      parentId: ghostGroupId,
      position: {
        x: node.position.x - groupRect.x,
        y: node.position.y - groupRect.y,
      },
      style: {
        ...node.style,
        ...GHOST_STYLE,
      },
      draggable: false,
      selectable: false,
    } as GhostNode

    ghostChildren.push(ghostNode)
    processedNodes.add(node.id)

    // If this is a group, also clone its descendants recursively
    if (node.type === NodeTypes.CLUSTER_NODE) {
      const descendants = getAllDescendants(node, allNodes)

      descendants.forEach((descendant) => {
        if (processedNodes.has(descendant.id)) return

        const descendantLabel = String(descendant.data?.label || descendant.id)

        // Find the parent of this descendant (could be nested group)
        const descendantParent = descendants.find((d) => d.id === descendant.parentId) || node

        const ghostDescendant: GhostNode = {
          ...descendant,
          id: `ghost-child-${descendant.id}`,
          data: {
            ...descendant.data,
            isGhost: true as const,
            label: descendantLabel,
            _originalNodeId: descendant.id,
          },
          // Parent is the ghost version of the original parent
          parentId: `ghost-child-${descendantParent.id}`,
          // Position stays relative to its parent (already relative in original)
          position: descendant.position,
          style: {
            ...descendant.style,
            ...GHOST_STYLE,
          },
          draggable: false,
          selectable: false,
        } as GhostNode

        ghostChildren.push(ghostDescendant)
        processedNodes.add(descendant.id)
      })
    }
  })

  // Group node MUST come first (React Flow requirement)
  return {
    nodes: [ghostGroupNode, ...ghostChildren],
    edges: [], // No edges needed during selection
  }
}

/**
 * Remove all ghost group nodes and children
 *
 * @param allNodes - All workspace nodes
 * @returns Nodes with ghost group and children removed
 */
export const removeGhostGroup = (allNodes: Node[]): Node[] => {
  return allNodes.filter((node) => !node.id.startsWith('ghost-group') && !node.id.startsWith('ghost-child-'))
}

/**
 * Create a simple ghost group node (legacy - for non-dynamic use)
 */
export const createGhostGroup = (id: string, label: string = i18n.t('workspace.ghost.group')): GhostNode => {
  return {
    ...GHOST_BASE,
    id: `ghost-${id}`,
    type: NodeTypes.CLUSTER_NODE,
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
    case EntityType.COMBINER: {
      // Create combiner with dummy edge node position
      const dummyEdgeNode = { position: { x: 0, y: 0 } } as Node
      return createGhostCombiner(id, dummyEdgeNode)
    }
    case EntityType.ASSET_MAPPER: {
      // Asset Mapper uses same structure as Combiner
      const dummyEdgeNode = { position: { x: 0, y: 0 } } as Node
      return createGhostAssetMapper(id, dummyEdgeNode)
    }
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
  label: string = i18n.t('workspace.ghost.adapter')
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
        connection: Status.connection.STATELESS,
        runtime: Status.runtime.STOPPED,
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
      protocol: i18n.t('workspace.ghost.device'),
      label: i18n.t('workspace.ghost.device'),
    },
    style: GHOST_STYLE_ENHANCED,
  }

  // Create edge from ADAPTER to EDGE node
  const edgeToEdge: GhostEdge = {
    id: `ghost-edge-adapter-to-edge-${id}`,
    source: `ghost-adapter-${id}`,
    target: IdStubs.EDGE_NODE,
    // targetHandle: 'Top', // EDGE node has this handle
    focusable: false,
    type: EdgeTypes.DYNAMIC_EDGE,
    animated: true,
    style: GHOST_EDGE_STYLE,
    markerEnd: {
      type: MarkerType.ArrowClosed,
      width: 20,
      height: 20,
      color: GHOST_COLOR_EDGE,
    },
    data: {
      isGhost: true,
    },
  }

  // Create edge from DEVICE to ADAPTER
  const edgeToDevice: GhostEdge = {
    id: `ghost-edge-device-to-adapter-${id}`,
    target: `ghost-device-${id}`,
    source: `ghost-adapter-${id}`,
    // No handles specified - ghost nodes use default positions
    focusable: false,
    type: EdgeTypes.DYNAMIC_EDGE,
    animated: true,
    style: GHOST_EDGE_STYLE,
    markerEnd: {
      type: MarkerType.ArrowClosed,
      width: 20,
      height: 20,
      color: GHOST_COLOR_EDGE,
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
 * Calculate ghost bridge position using the same algorithm as real nodes
 * Bridges appear below the EDGE node
 */
export const calculateGhostBridgePosition = (
  nbBridges: number,
  edgeNodePos: XYPosition
): { bridgePos: XYPosition; hostPos: XYPosition } => {
  // Calculate centered position for next bridge
  const totalBridges = nbBridges + 1
  const centerOffset = (totalBridges - 1) / 2

  const bridgePos = {
    x: edgeNodePos.x + POS_NODE_INC.x * (nbBridges - centerOffset),
    y: edgeNodePos.y + POS_NODE_INC.y,
  }

  const hostPos = {
    x: bridgePos.x,
    y: bridgePos.y + 250, // HOST is 250px below BRIDGE
  }

  return { bridgePos, hostPos }
}

/**
 * Create a complete ghost bridge group (BRIDGE + HOST + connections)
 * This provides a full preview of what will be created
 */
export const createGhostBridgeGroup = (
  id: string,
  nbBridges: number,
  edgeNode: Node,
  label: string = i18n.t('workspace.ghost.bridge')
): GhostNodeGroup => {
  const { bridgePos, hostPos } = calculateGhostBridgePosition(nbBridges, edgeNode.position)

  // Create BRIDGE ghost node
  const bridgeNode: GhostNode = {
    ...GHOST_BASE,
    id: `ghost-bridge-${id}`,
    type: NodeTypes.BRIDGE_NODE,
    position: bridgePos,
    sourcePosition: Position.Top,
    data: {
      isGhost: true,
      label,
      id: `ghost-bridge-${id}`,
      status: {
        connection: Status.connection.STATELESS,
        runtime: Status.runtime.STOPPED,
      },
    },
    style: GHOST_STYLE_ENHANCED,
  }

  // Create HOST ghost node
  const hostNode: GhostNode = {
    ...GHOST_BASE,
    id: `ghost-host-${id}`,
    type: NodeTypes.HOST_NODE,
    position: hostPos,
    targetPosition: Position.Top,
    data: {
      isGhost: true,
      label: i18n.t('workspace.ghost.host'),
    },
    style: GHOST_STYLE_ENHANCED,
  }

  // Create edge from BRIDGE to EDGE node
  const edgeToEdge: GhostEdge = {
    id: `ghost-edge-bridge-to-edge-${id}`,
    source: `ghost-bridge-${id}`,
    target: IdStubs.EDGE_NODE,
    targetHandle: 'Bottom',
    type: EdgeTypes.DYNAMIC_EDGE,
    focusable: false,
    animated: true,
    style: GHOST_EDGE_STYLE,
    markerEnd: {
      type: MarkerType.ArrowClosed,
      width: 20,
      height: 20,
      color: GHOST_COLOR_EDGE,
    },
    data: {
      isGhost: true,
    },
  }

  // Create edge from BRIDGE to HOST
  const edgeToHost: GhostEdge = {
    id: `ghost-edge-bridge-to-host-${id}`,
    source: `ghost-bridge-${id}`,
    target: `ghost-host-${id}`,
    sourceHandle: 'Bottom',
    type: EdgeTypes.DYNAMIC_EDGE,
    focusable: false,
    animated: true,
    style: GHOST_EDGE_STYLE,
    markerEnd: {
      type: MarkerType.ArrowClosed,
      width: 20,
      height: 20,
      color: GHOST_COLOR_EDGE,
    },
    data: {
      isGhost: true,
    },
  }

  return {
    nodes: [bridgeNode, hostNode],
    edges: [edgeToEdge, edgeToHost],
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
