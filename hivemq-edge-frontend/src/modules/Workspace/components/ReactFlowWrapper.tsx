import { type MouseEvent as ReactMouseEvent, useCallback, useEffect, useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import type { Node, NodePositionChange } from '@xyflow/react'
import { ReactFlow, Background, getIncomers, getOutgoers } from '@xyflow/react'
import { Box, useToast } from '@chakra-ui/react'
import debug from 'debug'

import '@xyflow/react/dist/style.css'

import './reactflow-chakra.fix.css'

import MiniMap from '@/components/react-flow/MiniMap.tsx'
import SuspenseOutlet from '@/components/SuspenseOutlet.tsx'
import { EdgeTypes, NodeTypes } from '@/modules/Workspace/types.ts'
import useGetFlowElements from '@/modules/Workspace/hooks/useGetFlowElements.ts'
import useWorkspaceStore from '@/modules/Workspace/hooks/useWorkspaceStore.ts'
import StatusListener from '@/modules/Workspace/components/controls/StatusListener.tsx'
import CanvasControls from '@/modules/Workspace/components/controls/CanvasControls.tsx'
import SelectionListener from '@/modules/Workspace/components/controls/SelectionListener.tsx'
import CanvasToolbar from '@/modules/Workspace/components/controls/CanvasToolbar.tsx'
import WizardProgressBar from '@/modules/Workspace/components/wizard/WizardProgressBar.tsx'
import GhostNodeRenderer from '@/modules/Workspace/components/wizard/GhostNodeRenderer.tsx'
import WizardSelectionRestrictions from '@/modules/Workspace/components/wizard/WizardSelectionRestrictions.tsx'
import WizardSelectionPanel from '@/modules/Workspace/components/wizard/WizardSelectionPanel.tsx'
import WizardConfigurationPanel from '@/modules/Workspace/components/wizard/WizardConfigurationPanel.tsx'
import { useWizardStore } from '@/modules/Workspace/hooks/useWizardStore'
import { useProtocolAdaptersContext } from '@/modules/Workspace/components/wizard/ProtocolAdaptersContext'
import MonitoringEdge from '@/modules/Workspace/components/edges/MonitoringEdge.tsx'
import {
  NodeAdapter,
  NodeBridge,
  NodeEdge,
  NodeGroup,
  NodeListener,
  NodeHost,
  NodeDevice,
  NodeCombiner,
  NodePulse,
} from '@/modules/Workspace/components/nodes'
import { DynamicEdge } from '@/modules/Workspace/components/edges/DynamicEdge'
import { getGluedPosition, gluedNodeDefinition } from '@/modules/Workspace/utils/nodes-utils.ts'
import { proOptions } from '@/components/react-flow/react-flow.utils.ts'

const debugLog = debug('workspace:wizard:selection')

const ReactFlowWrapper = () => {
  const { t } = useTranslation()
  const toast = useToast()
  const { isActive: isWizardActive } = useWizardStore((state) => ({ isActive: state.isActive }))
  const { protocolAdapters } = useProtocolAdaptersContext()
  const { nodes: newNodes, edges: newEdges } = useGetFlowElements()
  const nodeTypes = useMemo(
    () => ({
      [NodeTypes.CLUSTER_NODE]: NodeGroup,
      [NodeTypes.EDGE_NODE]: NodeEdge,
      [NodeTypes.ADAPTER_NODE]: NodeAdapter,
      [NodeTypes.BRIDGE_NODE]: NodeBridge,
      [NodeTypes.LISTENER_NODE]: NodeListener,
      [NodeTypes.HOST_NODE]: NodeHost,
      [NodeTypes.DEVICE_NODE]: NodeDevice,
      [NodeTypes.COMBINER_NODE]: NodeCombiner,
      [NodeTypes.PULSE_NODE]: NodePulse,
    }),
    []
  )
  const { nodes, edges, onNodesChange, onEdgesChange, onAddNodes, onAddEdges } = useWorkspaceStore()

  useEffect(() => {
    if (newNodes.length) onAddNodes(newNodes.map((node) => ({ item: node, type: 'add' })))
    if (newEdges.length) onAddEdges(newEdges.map((edge) => ({ item: edge, type: 'add' })))
  }, [newEdges, newNodes, onAddEdges, onAddNodes])

  const edgeTypes = useMemo(
    () => ({
      [EdgeTypes.REPORT_EDGE]: MonitoringEdge,
      [EdgeTypes.DYNAMIC_EDGE]: DynamicEdge,
    }),
    []
  )

  /**
   * Bug with the SHIFT+select
   * @see https://github.com/xyflow/xyflow/issues/4441
   */
  const onReactFlowNodeDrag = useCallback(
    (_event: ReactMouseEvent, _node: Node, draggedNodes: Node[]) => {
      const gluedDraggedNodes = draggedNodes.filter((node) =>
        Object.keys(gluedNodeDefinition).includes(node.type as NodeTypes)
      )

      const edge = nodes.find((e) => e.type === NodeTypes.EDGE_NODE)
      for (const movedNode of gluedDraggedNodes) {
        const [type, , handle] = gluedNodeDefinition[movedNode.type as NodeTypes]
        if (!type) continue

        const outgoers =
          handle === 'target' ? getOutgoers(movedNode, nodes, edges) : getIncomers(movedNode, nodes, edges)
        const gluedNode = outgoers.find((node) => node.type === type)
        if (!gluedNode) continue

        const positionChange: NodePositionChange = {
          id: gluedNode.id,
          type: 'position',
          position: getGluedPosition(movedNode, edge),
        }

        onNodesChange([positionChange])
      }
    },
    [edges, nodes, onNodesChange]
  )

  // Cleanup wizard on unmount to prevent orphaned state
  useEffect(() => {
    return () => {
      const { isActive, actions } = useWizardStore.getState()
      if (isActive) {
        actions.cancelWizard()
      }
    }
  }, [])

  /**
   * Handle node clicks during wizard selection mode
   */
  const onNodeClick = useCallback(
    (_event: ReactMouseEvent, node: Node) => {
      // Only handle clicks when wizard is active with selection constraints
      const { isActive, selectionConstraints, selectedNodeIds, actions } = useWizardStore.getState()

      if (!isActive || !selectionConstraints) {
        // Normal mode - let default behavior handle it
        debugLog('⏭️ Not in selection mode')
        return
      }

      // Check if node is selectable based on constraints
      const isGhost = node.data?.isGhost
      const isEdgeNode = node.id === 'EDGE_NODE'

      if (isGhost || isEdgeNode) {
        debugLog('🚫 Ghost or edge node - not selectable')
        return // Can't select ghost or edge nodes
      }

      // Check allowed types
      const { allowedNodeTypes = [], customFilter, requiresProtocolCapabilities } = selectionConstraints
      if (allowedNodeTypes.length > 0 && !allowedNodeTypes.includes(node.type || '')) {
        debugLog('🚫 Node type not allowed:', node.type, 'allowed:', allowedNodeTypes)
        return // Type not allowed
      }

      // Check protocol adapter capabilities for ADAPTER_NODE types
      if (node.type === 'ADAPTER_NODE' && requiresProtocolCapabilities && requiresProtocolCapabilities.length > 0) {
        const adapterType = node.data?.type

        if (!adapterType) {
          debugLog('🚫 Missing adapter type on node:', node.id)
          return
        }

        // If protocol adapters not loaded yet, skip capability check
        // WizardSelectionRestrictions handles visual filtering
        if (!protocolAdapters) {
          debugLog('⏳ Protocol adapters not loaded yet, skipping capability check')
        } else {
          // Protocol adapters loaded - check capabilities
          const protocolAdapter = protocolAdapters.find((p) => p.id === adapterType)
          if (!protocolAdapter || !protocolAdapter.capabilities) {
            debugLog('🚫 Protocol adapter not found or has no capabilities:', adapterType)
            return
          }

          const hasAllCapabilities = requiresProtocolCapabilities.every((cap) =>
            protocolAdapter.capabilities?.includes(cap)
          )

          if (!hasAllCapabilities) {
            debugLog('🚫 Adapter missing required capabilities:', {
              required: requiresProtocolCapabilities,
              has: protocolAdapter.capabilities,
            })
            return
          }
        }
      }

      // If custom filter provided, apply it
      if (customFilter && !customFilter(node)) {
        debugLog('🚫 Node filtered out by custom filter')
        return
      }

      // All checks passed - toggle selection
      const isSelected = selectedNodeIds.includes(node.id)
      debugLog(isSelected ? '➖ Deselecting node' : '➕ Selecting node')

      if (isSelected) {
        // Deselect
        actions.deselectNode(node.id)
      } else {
        // Check max constraint before selecting
        const { maxNodes = Infinity } = selectionConstraints
        if (selectedNodeIds.length >= maxNodes) {
          // Show toast: max reached
          toast({
            title: t('workspace.wizard.selection.maxReached'),
            description: t('workspace.wizard.selection.maxReachedDescription', { count: maxNodes }),
            status: 'warning',
            duration: 3000,
            isClosable: true,
          })
          return
        }

        // Select
        actions.selectNode(node.id)
      }
    },
    [t, toast, protocolAdapters]
    // protocolAdapters must be in deps to avoid stale closure when capabilities are checked
  )

  return (
    <ReactFlow
      id="edge-workspace-canvas"
      nodes={nodes}
      edges={edges}
      nodeTypes={nodeTypes}
      edgeTypes={edgeTypes}
      onNodesChange={onNodesChange}
      onEdgesChange={onEdgesChange}
      onNodeDrag={onReactFlowNodeDrag}
      onNodeClick={onNodeClick}
      fitView
      snapToGrid={true}
      nodesConnectable={false}
      deleteKeyCode={null}
      proOptions={proOptions}
      role="region"
      aria-label={t('workspace.canvas.aria-label')}
      // Disable interactions when wizard is active
      nodesDraggable={!isWizardActive}
      elementsSelectable={!isWizardActive}
      selectionOnDrag={!isWizardActive}
    >
      <Box role="group" aria-label={t('workspace.canvas.toolbar.container')} aria-controls="edge-workspace-canvas">
        <CanvasToolbar />
        <SelectionListener />
        <StatusListener />
        <CanvasControls />
        <WizardProgressBar />
        <GhostNodeRenderer />
        <WizardSelectionRestrictions />
        <WizardSelectionPanel />
        <MiniMap
          zoomable
          pannable
          nodeClassName={(node) => node.type || ''}
          nodeComponent={(miniMapNode) => {
            if (miniMapNode.className === NodeTypes.EDGE_NODE)
              return <circle cx={miniMapNode.x} cy={miniMapNode.y} r="50" fill="#ffc000" />
            if (miniMapNode.className === NodeTypes.HOST_NODE) return null
            return (
              <rect
                x={miniMapNode.x}
                y={miniMapNode.y}
                width={miniMapNode.width}
                height={miniMapNode.height}
                fill="#e2e2e2"
              />
            )
          }}
        />
      </Box>
      <Background />
      <SuspenseOutlet />
      <WizardConfigurationPanel />
    </ReactFlow>
  )
}

ReactFlowWrapper.propTypes = {}

export default ReactFlowWrapper
