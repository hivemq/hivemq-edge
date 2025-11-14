import type { FC } from 'react'
import { useEffect } from 'react'
import { useReactFlow, MarkerType } from '@xyflow/react'
import debug from 'debug'

import { useWizardState, useWizardGhosts } from '@/modules/Workspace/hooks/useWizardStore'
import { useListProtocolAdapters } from '@/api/hooks/useProtocolAdapters/useListProtocolAdapters'
import { useListBridges } from '@/api/hooks/useGetBridges/useListBridges'
import { calculateBarycenter } from '@/modules/Workspace/utils/nodes-utils'
import { requiresGhost } from './utils/wizardMetadata'
import {
  createGhostAdapterGroup,
  createGhostBridgeGroup,
  createGhostCombiner,
  createGhostAssetMapper,
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
  const { getNodes, setNodes, getEdges, setEdges, fitView } = useReactFlow()
  const { data: adapters } = useListProtocolAdapters()
  const { data: bridges } = useListBridges()

  // Add ghost nodes and edges to canvas when wizard becomes active
  useEffect(() => {
    if (!isActive || !entityType) {
      // Clean up ghost nodes and edges when wizard is not active
      // Always clean React Flow state, even if Zustand store is already empty
      const nodes = getNodes()
      const edges = getEdges()
      const realNodes = removeGhostNodes(nodes)
      const realEdges = removeGhostEdges(edges)

      // Only update if there are actually ghost nodes/edges to remove
      if (realNodes.length !== nodes.length || realEdges.length !== edges.length) {
        setNodes(realNodes)
        setEdges(realEdges)
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
      // Get EDGE node for positioning
      const edgeNode = getNodes().find((n) => n.id === IdStubs.EDGE_NODE)

      if (!edgeNode) {
        debugLog('EDGE node not found, cannot create ghost nodes')
        return
      }

      // Create ghost for entity type
      if (entityType === EntityType.ADAPTER) {
        const nbAdapters = adapters?.length || 0
        const ghostGroup = createGhostAdapterGroup('wizard-preview', nbAdapters, edgeNode)

        // Add to wizard store
        addGhostNodes(ghostGroup.nodes)
        addGhostEdges(ghostGroup.edges)

        // Add to React Flow
        const nodes = getNodes()
        const edges = getEdges()
        setNodes([...nodes, ...ghostGroup.nodes])
        setEdges([...edges, ...ghostGroup.edges])

        // Focus viewport on ghost nodes with animation
        setTimeout(() => {
          fitView({
            nodes: ghostGroup.nodes,
            duration: 800,
            padding: 0.3,
          })
        }, 100)
      } else if (entityType === EntityType.BRIDGE) {
        const nbBridges = bridges?.length || 0
        const ghostGroup = createGhostBridgeGroup('wizard-preview', nbBridges, edgeNode)

        // Add to wizard store
        addGhostNodes(ghostGroup.nodes)
        addGhostEdges(ghostGroup.edges)

        // Add to React Flow
        const nodes = getNodes()
        const edges = getEdges()
        setNodes([...nodes, ...ghostGroup.nodes])
        setEdges([...edges, ...ghostGroup.edges])

        // Focus viewport on ghost nodes with animation
        setTimeout(() => {
          fitView({
            nodes: ghostGroup.nodes,
            duration: 800,
            padding: 0.3,
          })
        }, 100)
      } else if (entityType === EntityType.COMBINER) {
        // Create single ghost combiner node
        const ghostNode = createGhostCombiner('wizard-preview', edgeNode)

        // Create ghost edge from combiner to EDGE node
        const ghostEdge: GhostEdge = {
          id: 'ghost-edge-combiner-to-edge',
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
            color: '#4299E1',
          },
          data: { isGhost: true },
        }

        // Add to wizard store
        addGhostNodes([ghostNode])
        addGhostEdges([ghostEdge])

        // Add to React Flow
        const nodes = getNodes()
        const edges = getEdges()
        setNodes([...nodes, ghostNode])
        setEdges([...edges, ghostEdge])

        // Focus viewport on ghost node
        setTimeout(() => {
          fitView({
            nodes: [ghostNode],
            duration: 800,
            padding: 0.3,
          })
        }, 100)
      } else if (entityType === EntityType.ASSET_MAPPER) {
        // Asset Mapper uses same ghost structure as Combiner (it IS a Combiner)
        const ghostNode = createGhostAssetMapper('wizard-preview', edgeNode)

        // Create ghost edge from asset mapper to EDGE node
        const ghostEdge: GhostEdge = {
          id: 'ghost-edge-assetmapper-to-edge',
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
            color: '#4299E1',
          },
          data: { isGhost: true },
        }

        // Add to wizard store
        addGhostNodes([ghostNode])
        addGhostEdges([ghostEdge])

        // Add to React Flow
        const nodes = getNodes()
        const edges = getEdges()
        setNodes([...nodes, ghostNode])
        setEdges([...edges, ghostEdge])

        // Focus viewport on ghost node
        setTimeout(() => {
          fitView({
            nodes: [ghostNode],
            duration: 800,
            padding: 0.3,
          })
        }, 100)
      }
      // TODO: Add other entity types (GROUP)
    } else if (ghostNodes.length > 0 || ghostEdges.length > 0) {
      // Wizard is active but not creating new ghosts - ensure existing ghosts are in React Flow
      const nodes = getNodes()
      const edges = getEdges()

      const nodesWithoutGhosts = removeGhostNodes(nodes)
      const edgesWithoutGhosts = removeGhostEdges(edges)

      // Add missing ghost nodes
      const nodeIds = new Set(nodes.map((n) => n.id))
      const missingGhosts = ghostNodes.filter((g) => !nodeIds.has(g.id))

      // Add missing ghost edges
      const edgeIds = new Set(edges.map((e) => e.id))
      const missingEdges = ghostEdges.filter((g) => !edgeIds.has(g.id))

      if (missingGhosts.length > 0 || missingEdges.length > 0) {
        setNodes([...nodesWithoutGhosts, ...ghostNodes])
        setEdges([...edgesWithoutGhosts, ...ghostEdges])
      }
    }
  }, [
    isActive,
    entityType,
    currentStep,
    ghostNodes,
    ghostEdges,
    adapters,
    addGhostNodes,
    addGhostEdges,
    clearGhostNodes,
    getNodes,
    setNodes,
    getEdges,
    setEdges,
    fitView,
    bridges,
  ])

  // Update ghost combiner position based on selected sources
  useEffect(() => {
    // Only for combiner and asset mapper wizards
    if (!isActive || (entityType !== EntityType.COMBINER && entityType !== EntityType.ASSET_MAPPER)) {
      return
    }

    // Need at least one source selected
    if (!selectedNodeIds || selectedNodeIds.length === 0) {
      return
    }

    // Find the ghost combiner/asset mapper node
    const nodes = getNodes()
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

    // Update ghost position to barycenter (slightly above sources)
    const updatedNodes = nodes.map((node) => {
      if (node.id === ghostCombinerNode.id) {
        return {
          ...node,
          position: {
            x: barycenter.x,
            y: barycenter.y, //- POS_NODE_INC.y * 0.5,
          },
          // Add smooth transition animation
          style: {
            ...node.style,
            transition: 'transform 0.3s ease-out',
          },
        }
      }
      return node
    })

    setNodes(updatedNodes)

    // Update ghost edges from selected sources to ghost combiner
    const edges = getEdges()
    const realEdges = removeGhostEdges(edges)

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
    setEdges([...realEdges, ...newGhostEdges])
  }, [isActive, entityType, selectedNodeIds, getNodes, setNodes, getEdges, setEdges, ghostEdges])

  // Cleanup on unmount
  useEffect(() => {
    return () => {
      if (ghostNodes.length > 0 || ghostEdges.length > 0) {
        const nodes = getNodes()
        const edges = getEdges()
        const realNodes = removeGhostNodes(nodes)
        const realEdges = removeGhostEdges(edges)
        setNodes(realNodes)
        setEdges(realEdges)
      }
    }
  }, []) // eslint-disable-line react-hooks/exhaustive-deps

  // This component doesn't render anything, it just manages state
  return null
}

export default GhostNodeRenderer
