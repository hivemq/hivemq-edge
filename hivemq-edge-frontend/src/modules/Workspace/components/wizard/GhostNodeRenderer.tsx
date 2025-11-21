import type { FC } from 'react'
import { useEffect, useRef } from 'react'
import { useReactFlow, MarkerType } from '@xyflow/react'
import debug from 'debug'

import { useWizardState, useWizardGhosts, useWizardStore } from '@/modules/Workspace/hooks/useWizardStore'
import useWorkspaceStore from '@/modules/Workspace/hooks/useWorkspaceStore'
import { useListProtocolAdapters } from '@/api/hooks/useProtocolAdapters/useListProtocolAdapters'
import { useListBridges } from '@/api/hooks/useGetBridges/useListBridges'
import { calculateBarycenter } from '@/modules/Workspace/utils/nodes-utils'
import { requiresGhost } from './utils/wizardMetadata'
import type { GhostNodeGroup } from './utils/ghostNodeFactory'
import { createGhostCombinerGroup } from './utils/ghostNodeFactory'
import {
  createGhostAdapterGroup,
  createGhostBridgeGroup,
  removeGhostNodes,
  removeGhostEdges,
  GHOST_EDGE_STYLE,
} from './utils/ghostNodeFactory'
import { EntityType, type GhostEdge } from './types'
import { IdStubs, EdgeTypes } from '@/modules/Workspace/types'

const debugLog = debug('workspace:wizard:ghostnode')

/**
 * Component that manages ghost node rendering
 * Adds/removes ghost nodes based on wizard state
 */
const GhostNodeRenderer: FC = () => {
  const { isActive, entityType, currentStep, selectedNodeIds } = useWizardState()
  const { ghostNodes, ghostEdges, addGhostNodes, addGhostEdges, clearGhostNodes } = useWizardGhosts()
  const { nodes, edges, onAddNodes, onAddEdges, onNodesChange, onEdgesChange } = useWorkspaceStore()
  const { fitView } = useReactFlow()
  const { data: adapters } = useListProtocolAdapters()
  const { data: bridges } = useListBridges()

  // Track previous selection to avoid infinite loops when updating edges
  const prevSelectedNodeIdsRef = useRef<string[]>([])

  // Add ghost nodes and edges to canvas when wizard becomes active
  useEffect(() => {
    if (!isActive || !entityType) {
      // Clean up ghost nodes and edges when wizard is not active
      const realNodes = removeGhostNodes(nodes)
      const realEdges = removeGhostEdges(edges)

      // Only update if there are actually ghost nodes/edges to remove
      if (realNodes.length !== nodes.length) {
        // Remove ghost nodes from workspace
        const ghostNodeIds = nodes.filter((n) => !realNodes.find((rn) => rn.id === n.id)).map((n) => n.id)
        onNodesChange(ghostNodeIds.map((id) => ({ id, type: 'remove' })))
      }

      if (realEdges.length !== edges.length) {
        // Remove ghost edges from workspace
        const ghostEdgeIds = edges.filter((e) => !realEdges.find((re) => re.id === e.id)).map((e) => e.id)
        onEdgesChange(ghostEdgeIds.map((id) => ({ id, type: 'remove' })))
      }

      // Clear store if not already empty
      if (ghostNodes.length > 0 || ghostEdges.length > 0) {
        clearGhostNodes()
      }
      return
    }

    // Check if this entity type requires ghost nodes
    if (!requiresGhost(entityType)) {
      return
    }

    // Ghost nodes stay visible throughout the wizard
    // They are only removed on wizard completion or cancellation

    // Create ghost group if we don't have one yet
    if (ghostNodes.length === 0) {
      // Get EDGE node for positioning from workspace store
      const edgeNode = nodes.find((n) => n.id === IdStubs.EDGE_NODE)

      if (!edgeNode) {
        debugLog('EDGE node not found, cannot create ghost nodes')
        return
      }

      /**
       * Create the ghost group
       */
      const addGhostGroup = (ghostGroup: GhostNodeGroup) => {
        // Add to wizard store
        addGhostNodes(ghostGroup.nodes)
        addGhostEdges(ghostGroup.edges)

        // Add to workspace store (which manages React Flow nodes)
        onAddNodes(ghostGroup.nodes.map((node) => ({ item: node, type: 'add' })))
        onAddEdges(ghostGroup.edges.map((edge) => ({ item: edge, type: 'add' })))

        // Focus viewport on ghost nodes with animation
        setTimeout(() => {
          fitView({
            nodes: ghostGroup.nodes,
            duration: 800,
            padding: 0.3,
          })
        }, 100)
      }

      // Create ghost for entity type
      if (entityType === EntityType.ADAPTER) {
        const nbAdapters = adapters?.length || 0
        const ghostGroup = createGhostAdapterGroup('wizard-preview', nbAdapters, edgeNode)

        addGhostGroup(ghostGroup)
      } else if (entityType === EntityType.BRIDGE) {
        const nbBridges = bridges?.length || 0
        const ghostGroup = createGhostBridgeGroup('wizard-preview', nbBridges, edgeNode)

        addGhostGroup(ghostGroup)
      } else if (entityType === EntityType.COMBINER) {
        const ghostGroup = createGhostCombinerGroup('wizard-preview', edgeNode, entityType)

        addGhostGroup(ghostGroup)
      } else if (entityType === EntityType.ASSET_MAPPER) {
        const ghostGroup = createGhostCombinerGroup('wizard-preview', edgeNode, entityType)

        addGhostGroup(ghostGroup)
      }
      // TODO: Add other entity types (GROUP)
    } else if (ghostNodes.length > 0 || ghostEdges.length > 0) {
      // Add missing ghost nodes/edges if they were removed from workspace
      const nodeIds = new Set(nodes.map((n) => n.id))
      const missingGhosts = ghostNodes.filter((g) => !nodeIds.has(g.id))

      const edgeIds = new Set(edges.map((e) => e.id))
      const missingEdges = ghostEdges.filter((g) => !edgeIds.has(g.id))

      if (missingGhosts.length > 0) {
        onAddNodes(missingGhosts.map((node) => ({ item: node, type: 'add' })))
      }

      if (missingEdges.length > 0) {
        onAddEdges(missingEdges.map((edge) => ({ item: edge, type: 'add' })))
      }
    }
  }, [
    isActive,
    entityType,
    currentStep,
    ghostNodes,
    ghostEdges,
    adapters,
    bridges,
    nodes,
    edges,
    addGhostNodes,
    addGhostEdges,
    clearGhostNodes,
    onAddNodes,
    onAddEdges,
    onNodesChange,
    onEdgesChange,
    fitView,
  ])

  // Update ghost combiner position and edges based on selected sources
  useEffect(() => {
    // Only for combiner and asset mapper wizards
    if (!isActive || (entityType !== EntityType.COMBINER && entityType !== EntityType.ASSET_MAPPER)) {
      prevSelectedNodeIdsRef.current = []
      return
    }

    // Need at least one source selected
    if (!selectedNodeIds || selectedNodeIds.length === 0) {
      prevSelectedNodeIdsRef.current = []
      return
    }

    // Check if selection actually changed (avoid infinite loop)
    const selectionChanged =
      prevSelectedNodeIdsRef.current.length !== selectedNodeIds.length ||
      selectedNodeIds.some((id) => !prevSelectedNodeIdsRef.current.includes(id))

    if (!selectionChanged) {
      return // Selection hasn't changed, don't update
    }

    // Update ref with current selection
    prevSelectedNodeIdsRef.current = [...selectedNodeIds]

    // Find the ghost combiner/asset mapper node
    const ghostCombinerNode = nodes.find(
      (n) => n.id.startsWith('ghost-combiner-') || n.id.startsWith('ghost-assetmapper-')
    )

    if (!ghostCombinerNode) {
      return // Ghost not created yet
    }

    // Get selected source nodes with their CURRENT positions
    const selectedNodes = nodes.filter((n) => selectedNodeIds.includes(n.id))

    if (selectedNodes.length === 0) {
      return
    }

    // Calculate barycenter of selected sources
    const barycenter = calculateBarycenter(selectedNodes)

    // Update ghost position to barycenter using workspace store
    onNodesChange([
      {
        id: ghostCombinerNode.id,
        type: 'position',
        position: {
          x: barycenter.x,
          y: barycenter.y,
        },
      },
    ])

    // Keep the edge from ghost combiner to EDGE node
    const ghostToEdgeEdge = ghostEdges.find((e) => e.source === ghostCombinerNode.id && e.target === IdStubs.EDGE_NODE)

    // Create ghost edges from each selected source to ghost combiner
    const ghostEdgesFromSources: GhostEdge[] = selectedNodes.map((sourceNode, index) => ({
      id: `ghost-edge-source-${index}-${sourceNode.id}`,
      source: sourceNode.id,
      target: ghostCombinerNode.id,
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

    // Combine all edges
    const newGhostEdges = ghostToEdgeEdge ? [ghostToEdgeEdge, ...ghostEdgesFromSources] : ghostEdgesFromSources

    // Remove old combiner-related ghost edges
    const currentCombinerEdges = edges.filter(
      (e) => e.data?.isGhost && (e.target === ghostCombinerNode.id || e.source === ghostCombinerNode.id)
    )

    const edgesToRemove = currentCombinerEdges.map((e) => e.id)
    if (edgesToRemove.length > 0) {
      onEdgesChange(edgesToRemove.map((id) => ({ id, type: 'remove' })))
    }

    // Add new edges
    onAddEdges(newGhostEdges.map((edge) => ({ item: edge, type: 'add' })))
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isActive, entityType, selectedNodeIds, nodes, onNodesChange, onEdgesChange, onAddEdges])
  // Note: edges and ghostEdges intentionally omitted from deps to prevent infinite loop

  // Cleanup on unmount
  useEffect(() => {
    return () => {
      const { ghostNodes: currentGhostNodes, ghostEdges: currentGhostEdges } = useWizardStore.getState()
      if (currentGhostNodes.length > 0 || currentGhostEdges.length > 0) {
        const {
          nodes: currentNodes,
          edges: currentEdges,
          onNodesChange: nodesChange,
          onEdgesChange: edgesChange,
        } = useWorkspaceStore.getState()
        const realNodes = removeGhostNodes(currentNodes)
        const realEdges = removeGhostEdges(currentEdges)

        const ghostNodeIds = currentNodes.filter((n) => !realNodes.find((rn) => rn.id === n.id)).map((n) => n.id)
        const ghostEdgeIds = currentEdges.filter((e) => !realEdges.find((re) => re.id === e.id)).map((e) => e.id)

        if (ghostNodeIds.length > 0) {
          nodesChange(ghostNodeIds.map((id) => ({ id, type: 'remove' })))
        }
        if (ghostEdgeIds.length > 0) {
          edgesChange(ghostEdgeIds.map((id) => ({ id, type: 'remove' })))
        }
      }
    }
  }, [])

  // This component doesn't render anything, it just manages state
  return null
}

export default GhostNodeRenderer
