/**
 * Phantom Node Factory
 *
 * Helper functions to create temporary "phantom" nodes for wizard mode.
 * These nodes provide the data structure needed by components like CombinerMappingManager
 * before the actual entity is created via API.
 */

import type { Node } from '@xyflow/react'
import type { Combiner, EntityReference } from '@/api/__generated__'
import { EntityType } from '@/api/__generated__'
import { NodeTypes } from '@/modules/Workspace/types'
import useWorkspaceStore from '@/modules/Workspace/hooks/useWorkspaceStore'

/**
 * Create a phantom combiner node from selected node IDs
 * Used during wizard to provide a temporary node structure before API creation
 */
export const createPhantomCombinerNode = (selectedNodeIds: string[], name: string = 'New Combiner'): Node<Combiner> => {
  const nodes = useWorkspaceStore.getState().nodes

  // Map selected node IDs to EntityReferences
  const sourceEntities: EntityReference[] = selectedNodeIds.map((nodeId) => {
    const node = nodes.find((n) => n.id === nodeId)
    return {
      id: nodeId,
      type: inferEntityTypeFromNode(node),
    }
  })

  return {
    id: 'phantom-combiner-wizard',
    type: NodeTypes.COMBINER_NODE,
    position: { x: 0, y: 0 }, // Position doesn't matter - not rendered
    data: {
      id: 'phantom-combiner-wizard',
      name,
      description: '',
      sources: {
        items: sourceEntities,
      },
      mappings: {
        items: [], // Empty initially - user will configure
      },
    },
    selected: false,
    dragging: false,
  } as Node<Combiner>
}

/**
 * Infer entity type from workspace node type
 */
const inferEntityTypeFromNode = (node: Node | undefined): EntityType => {
  if (!node) {
    return EntityType.EDGE_BROKER
  }

  switch (node.type) {
    case NodeTypes.ADAPTER_NODE:
      return EntityType.ADAPTER
    case NodeTypes.BRIDGE_NODE:
      return EntityType.BRIDGE
    case NodeTypes.EDGE_NODE:
      return EntityType.EDGE_BROKER
    case NodeTypes.PULSE_NODE:
      return EntityType.PULSE_AGENT
    default:
      return EntityType.EDGE_BROKER
  }
}
