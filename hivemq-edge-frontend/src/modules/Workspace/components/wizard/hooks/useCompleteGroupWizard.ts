import { DEFAULT_TOAST_OPTION } from '@/hooks/useEdgeToast/toast-utils.ts'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useToast } from '@chakra-ui/react'
import { useReactFlow } from '@xyflow/react'

import { useWizardStore } from '@/modules/Workspace/hooks/useWizardStore'
import { getGroupBounds } from '@/modules/Workspace/utils/group.utils'
import { removeGhostGroup } from '../utils/ghostNodeFactory'
import {
  getAutoIncludedNodes,
  validateGroupHierarchy,
  getMaxChildDepth,
  MAX_NESTING_DEPTH,
} from '../utils/groupConstraints'
import useWorkspaceStore from '@/modules/Workspace/hooks/useWorkspaceStore'
import { NodeTypes } from '@/modules/Workspace/types.ts'

export const useCompleteGroupWizard = () => {
  const { t } = useTranslation()
  const toast = useToast()
  const { getNodes, setNodes, getNodesBounds, getEdges } = useReactFlow()
  const [isCompleting, setIsCompleting] = useState(false)
  const { onAddEdges } = useWorkspaceStore()

  const completeWizard = async () => {
    setIsCompleting(true)

    try {
      // Read directly from store to avoid stale closure
      const { configurationData, selectedNodeIds } = useWizardStore.getState()
      const { groupConfig } = configurationData

      if (!groupConfig || !selectedNodeIds || selectedNodeIds.length < 2) {
        const missing = []
        if (!groupConfig) missing.push('group configuration')
        if (!selectedNodeIds || selectedNodeIds.length < 2) missing.push('selected nodes (minimum 2)')
        throw new Error(`Missing configuration data: ${missing.join(', ')}`)
      }

      // Get selected nodes (the ones user manually selected)
      const allNodes = getNodes()
      const allEdges = getEdges()
      const selectedNodes = allNodes.filter((n) => selectedNodeIds.includes(n.id) && !n.data?.isGhost)

      if (selectedNodes.length < 2) {
        throw new Error('At least 2 nodes must be selected to create a group')
      }

      // Filter to only include adapters and bridges (not devices/hosts)
      // Groups should only contain top-level entities
      const groupCandidates = selectedNodes.filter(
        (node) =>
          node.type === NodeTypes.ADAPTER_NODE ||
          node.type === NodeTypes.BRIDGE_NODE ||
          node.type === NodeTypes.CLUSTER_NODE
      )

      if (groupCandidates.length === 0) {
        throw new Error('No valid nodes to group (must select adapters, bridges, or groups)')
      }

      // Get auto-included DEVICE and HOST nodes
      // This only includes devices/hosts for top-level adapters/bridges
      // Groups are NOT traversed - their children remain as children of the nested group
      const autoIncludedNodes = getAutoIncludedNodes(groupCandidates, allNodes, allEdges)

      // ALL nodes that will be DIRECT children of the new group
      // This includes:
      // - Manually selected adapters/bridges/groups (groupCandidates)
      // - Auto-included devices/hosts for those adapters/bridges (autoIncludedNodes)
      // - Does NOT include children of nested groups (they stay in their parent group)
      const allGroupNodes = [...groupCandidates, ...autoIncludedNodes]

      // Validate nesting depth before creating group
      // Check if any selected group would cause max depth to be exceeded
      for (const node of groupCandidates) {
        if (node.type === NodeTypes.CLUSTER_NODE) {
          const childDepth = getMaxChildDepth(node, allNodes)
          if (childDepth + 1 > MAX_NESTING_DEPTH) {
            throw new Error(
              `Cannot create group: Maximum nesting depth (${MAX_NESTING_DEPTH} levels) would be exceeded by "${node.data.title || node.id}"`
            )
          }
        }
      }

      // Calculate bounding box for ALL nodes that will be direct children
      // For nested groups, this calculates the boundary of the entire group including its contents
      const rect = getNodesBounds(allGroupNodes)
      const groupRect = getGroupBounds(rect)

      // Create group node manually (can't use createGroup util which expects adapters only)
      const groupId = `GROUP_NODE@${groupCandidates.map((node) => node.data.id).join('+')}`

      // Type assertion for groupConfig
      const groupConfigTyped = groupConfig as { title: string; colorScheme: string }

      const newGroupNode = {
        id: groupId,
        type: 'CLUSTER_NODE' as const,
        data: {
          childrenNodeIds: allGroupNodes.map((node) => node.id), // Include ALL nodes
          title: groupConfigTyped.title,
          colorScheme: groupConfigTyped.colorScheme,
          isOpen: true,
        },
        style: {
          width: groupRect.width,
          height: groupRect.height,
        },
        position: { x: groupRect.x, y: groupRect.y },
      }

      // Create edge to EDGE_NODE
      const newGroupEdge = {
        id: `edge-${groupId}-EDGE_NODE`,
        source: groupId,
        target: 'EDGE_NODE',
        targetHandle: 'Top',
        type: 'DYNAMIC_EDGE' as const,
        hidden: true,
        focusable: false,
        markerEnd: {
          type: 'arrowclosed' as const,
          width: 20,
          height: 20,
          color: '#4299E1',
        },
        style: {
          strokeWidth: 1.5,
          stroke: '#4299E1',
        },
      }

      // Update ALL nodes to be children of the group (selected + auto-included)
      const updatedNodes = allNodes.map((node) => {
        if (allGroupNodes.some((gn) => gn.id === node.id)) {
          // This node should be in the group (either selected or auto-included)
          return {
            ...node,
            parentId: newGroupNode.id,
            extent: 'parent' as const,
            position: {
              // Position relative to group origin
              x: node.position.x - groupRect.x,
              y: node.position.y - groupRect.y,
            },
          }
        }
        return node
      })

      // Remove ghost nodes before adding real group
      const nodesWithoutGhosts = removeGhostGroup(updatedNodes)

      // Add the group node FIRST (React Flow requirement for groups)
      // Then update all nodes with parent relationships
      const finalNodes = [newGroupNode, ...nodesWithoutGhosts]

      // Validate group hierarchy before committing
      const validation = validateGroupHierarchy(finalNodes)
      if (!validation.valid) {
        console.error('Group hierarchy validation failed:', validation.errors)
        throw new Error(`Invalid group hierarchy: ${validation.errors[0]}`)
      }

      setNodes(finalNodes)

      // Add the group edge
      onAddEdges([{ item: newGroupEdge, type: 'add' }])

      toast({
        ...DEFAULT_TOAST_OPTION,
        title: t('workspace.wizard.group.success.title'),
        description: t('workspace.wizard.group.success.description', { title: groupConfigTyped.title }),
      })

      // TRANSITION SEQUENCE: Remove ghost, close wizard
      // Note: For groups, we don't use the standard transition sequence
      // because we're creating client-side only (no API call)
      // The group already exists at this point
      await new Promise((resolve) => setTimeout(resolve, 100))

      // Close wizard
      const { actions } = useWizardStore.getState()
      actions.cancelWizard()

      // Highlight the new group briefly (green glow for visual feedback)
      setTimeout(() => {
        const highlightedNodes = getNodes().map((node) => {
          if (node.id === newGroupNode.id) {
            return {
              ...node,
              style: {
                ...node.style,
                boxShadow: '0 0 0 4px rgba(72, 187, 120, 0.6), 0 0 20px rgba(72, 187, 120, 0.8)',
                transition: 'box-shadow 0.3s ease',
              },
            }
          }
          return node
        })

        setNodes(highlightedNodes)

        // Remove highlight after 2 seconds
        setTimeout(() => {
          const normalNodes = getNodes().map((node) => {
            if (node.id === newGroupNode.id && node.style?.boxShadow) {
              const { boxShadow, ...restStyle } = node.style
              return {
                ...node,
                style: restStyle,
              }
            }
            return node
          })

          setNodes(normalNodes)
        }, 2000)
      }, 200)
    } catch (error) {
      // Show error toast
      toast({
        ...DEFAULT_TOAST_OPTION,
        title: t('workspace.wizard.group.error.title'),
        description: error instanceof Error ? error.message : t('workspace.wizard.group.error.message'),
        status: 'error',
      })

      console.error('Failed to complete group wizard:', error)
    } finally {
      setIsCompleting(false)
    }
  }

  return { completeWizard, isCompleting }
}
