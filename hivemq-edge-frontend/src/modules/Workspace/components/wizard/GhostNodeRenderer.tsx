/**
 * Ghost Node Renderer
 *
 * Renders ghost nodes and edges on the canvas during wizard flow.
 * Ghost nodes provide a preview of what will be created.
 */

import type { FC } from 'react'
import { useEffect } from 'react'
import { useReactFlow } from '@xyflow/react'

import { useWizardState, useWizardGhosts } from '@/modules/Workspace/hooks/useWizardStore'
import { useListProtocolAdapters } from '@/api/hooks/useProtocolAdapters/useListProtocolAdapters'
import { requiresGhost } from './utils/wizardMetadata'
import { createGhostAdapterGroup, removeGhostNodes, removeGhostEdges } from './utils/ghostNodeFactory'
import { EntityType } from './types'
import { IdStubs } from '@/modules/Workspace/types'

/**
 * Component that manages ghost node rendering
 * Adds/removes ghost nodes based on wizard state
 */
const GhostNodeRenderer: FC = () => {
  const { isActive, entityType, currentStep } = useWizardState()
  const { ghostNodes, ghostEdges, addGhostNodes, addGhostEdges, clearGhostNodes } = useWizardGhosts()
  const { getNodes, setNodes, getEdges, setEdges, fitView } = useReactFlow()
  const { data: adapters } = useListProtocolAdapters()

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
        console.warn('EDGE node not found, cannot create ghost nodes')
        return
      }

      // Only create ghost for ADAPTER (multi-node support)
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
      }
      // TODO: Add other entity types (BRIDGE, etc.)
    } else {
      // Ensure ghost nodes and edges are in React Flow
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
  ])

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
