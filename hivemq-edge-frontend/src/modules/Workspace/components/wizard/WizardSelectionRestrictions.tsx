import type { FC } from 'react'
import { useEffect, useMemo } from 'react'
import { useReactFlow, MarkerType } from '@xyflow/react'
import type { Node, Edge } from '@xyflow/react'
import debug from 'debug'

import type { ProtocolAdapter } from '@/api/__generated__'
import { useWizardState } from '@/modules/Workspace/hooks/useWizardStore'
import { EdgeTypes, NodeTypes } from '@/modules/Workspace/types'
import type { SelectionConstraints } from './types'
import { GHOST_EDGE_STYLE } from './utils/ghostNodeFactory'
import { useProtocolAdaptersContext } from './ProtocolAdaptersContext'

const debugLog = debug('workspace:wizard:constraints')

/**
 * Check if a node can be selected based on constraints
 */
const checkConstraints = (
  node: Node,
  constraints: SelectionConstraints,
  protocolAdapters?: ProtocolAdapter[]
): boolean => {
  // Ghost nodes are never selectable
  if (node.data?.isGhost) {
    return false
  }

  // EDGE node is never selectable
  if (node.id === 'EDGE_NODE') {
    return false
  }

  // Check allowed node types
  if (constraints.allowedNodeTypes && constraints.allowedNodeTypes.length > 0) {
    if (!constraints.allowedNodeTypes.includes(node.type || '')) {
      return false
    }
  }

  // Check protocol adapter capabilities for ADAPTER_NODE types
  if (
    node.type === NodeTypes.ADAPTER_NODE &&
    constraints.requiresProtocolCapabilities &&
    constraints.requiresProtocolCapabilities.length > 0
  ) {
    const adapterType = node.data?.type // e.g., "opcua", "mqtt", etc.

    if (!adapterType || !protocolAdapters) {
      debugLog('[WizardSelection] Missing adapterType or protocolAdapters')
      return false
    }

    const protocolAdapter = protocolAdapters.find((p) => p.id && p.id === adapterType)

    if (!protocolAdapter || !protocolAdapter.capabilities) {
      debugLog('[WizardSelection] No protocol adapter or capabilities found')
      return false
    }

    // Check if adapter has ALL required capabilities
    const hasAllCapabilities = constraints.requiresProtocolCapabilities.every((cap) =>
      protocolAdapter.capabilities?.includes(cap)
    )

    if (!hasAllCapabilities) {
      debugLog('[WizardSelection] Protocol adapter does not meet required capabilities')
      return false
    }
  }

  // Apply custom filter if provided (e.g., adapter capabilities)
  if (constraints.customFilter) {
    return constraints.customFilter(node)
  }

  return true
}

/**
 * Component that applies visual restrictions and manages ghost edges during selection
 */
const WizardSelectionRestrictions: FC = () => {
  const { isActive, selectionConstraints, selectedNodeIds, currentStep } = useWizardState()
  const { getNodes, setNodes, getEdges, setEdges } = useReactFlow()
  const { protocolAdapters: protocolAdaptersList } = useProtocolAdaptersContext()

  // Enhanced constraints with protocol adapters injected (used locally only)
  const enhancedConstraints = useMemo(() => {
    if (!selectionConstraints) return null
    return {
      ...selectionConstraints,
      _protocolAdapters: protocolAdaptersList,
    }
  }, [selectionConstraints, protocolAdaptersList])

  // Manage node visibility based on constraints
  useEffect(() => {
    const nodes = getNodes()

    // If wizard not active, restore all nodes without ghosts (GhostNodeRenderer will handle ghost cleanup)
    if (!isActive) {
      const realNodes = nodes.filter((node) => !node.data?.isGhost)
      const restoredNodes = realNodes.map((node) => ({
        ...node,
        hidden: false,
        selectable: true,
        style: {
          ...node.style,
          cursor: 'grab',
          border: undefined,
          pointerEvents: undefined,
        },
      }))
      setNodes(restoredNodes)
      return
    }

    // If wizard active but no selection constraints, restore real nodes but keep ghosts visible
    if (!enhancedConstraints) {
      const restoredNodes = nodes.map((node) => {
        // Keep ghost nodes as-is
        if (node.data?.isGhost) {
          return node
        }
        // Restore real nodes to normal
        return {
          ...node,
          hidden: false,
          selectable: true,
          style: {
            ...node.style,
            cursor: 'grab',
            border: undefined,
            pointerEvents: undefined,
          },
        }
      })
      setNodes(restoredNodes)
      return
    }

    // Apply selection constraints - hide non-targets
    const constrainedNodes = nodes.map((node) => {
      const isAllowed = checkConstraints(node, enhancedConstraints, enhancedConstraints._protocolAdapters)
      const isGhost = node.data?.isGhost
      const isEdge = node.id === 'EDGE_NODE'

      // Ghost nodes: keep visible with ghost styling
      if (isGhost) {
        return {
          ...node,
          hidden: false,
          selectable: false,
          style: {
            ...node.style,
            cursor: 'default',
          },
        }
      }

      // EDGE node: keep visible but not selectable
      if (isEdge) {
        return {
          ...node,
          hidden: false,
          selectable: false,
          style: {
            ...node.style,
            cursor: 'default',
          },
        }
      }

      // Selection targets: visible, highlighted, clickable
      if (isAllowed) {
        return {
          ...node,
          hidden: false,
          selectable: false, // We handle clicks manually
          style: {
            ...node.style,
            cursor: 'pointer',
            border: '2px solid #4299E1', // Highlight available targets
            transition: 'border 0.2s ease',
            pointerEvents: 'all' as const, // Ensure nodes are clickable
          },
        }
      }

      // Non-targets: HIDE completely
      return {
        ...node,
        hidden: true,
      }
    })

    setNodes(constrainedNodes)
  }, [isActive, enhancedConstraints, currentStep, getNodes, setNodes])

  // Manage ghost edges based on selection
  useEffect(() => {
    // Only remove ghost edges when wizard is completely inactive
    if (!isActive) {
      const edges = getEdges()
      const realEdges = edges.filter((e) => !e.data?.isGhost)
      if (realEdges.length !== edges.length) {
        setEdges(realEdges)
      }
      return
    }

    // If wizard is active but not in selection mode, keep existing ghost edges
    if (!selectionConstraints) {
      return // Don't modify edges - keep them as-is
    }

    // During selection: manage ghost edges
    if (selectedNodeIds.length === 0) {
      // No selection yet - remove any selection ghost edges (keep combiner→edge ghost)
      const edges = getEdges()
      const nonSelectionGhostEdges = edges.filter((e) => !e.data?.isGhost || e.id === 'ghost-edge-combiner-to-edge')
      if (nonSelectionGhostEdges.length !== edges.length) {
        setEdges(nonSelectionGhostEdges)
      }
      return
    }

    const nodes = getNodes()
    const edges = getEdges()

    // Find ghost combiner node
    const ghostCombiner = nodes.find((n) => n.id.startsWith('ghost-combiner-'))
    if (!ghostCombiner) {
      return // No ghost combiner yet
    }

    // Create ghost edges for each selected node
    const ghostEdges: Edge[] = selectedNodeIds.map((nodeId) => ({
      id: `ghost-edge-${nodeId}-to-combiner`,
      source: nodeId,
      target: ghostCombiner.id,
      type: EdgeTypes.DYNAMIC_EDGE,
      animated: true,
      focusable: false,
      style: GHOST_EDGE_STYLE,
      markerEnd: {
        type: MarkerType.ArrowClosed,
        width: 20,
        height: 20,
        color: '#4299E1',
      },
      data: { isGhost: true },
    }))

    // Keep only real edges + new ghost edges
    const realEdges = edges.filter((e) => !e.data?.isGhost)
    setEdges([...realEdges, ...ghostEdges])
  }, [selectedNodeIds, isActive, selectionConstraints, getNodes, getEdges, setEdges])

  // This component doesn't render anything
  return null
}

export default WizardSelectionRestrictions
