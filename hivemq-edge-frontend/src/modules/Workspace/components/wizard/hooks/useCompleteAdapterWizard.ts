/**
 * useCompleteAdapterWizard Hook
 *
 * Handles the final step of the adapter wizard:
 * - Creates adapter via API
 * - Removes ghost nodes/edges
 * - Shows success/error feedback
 * - Completes wizard
 */

import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useToast } from '@chakra-ui/react'
import { useReactFlow } from '@xyflow/react'

import type { Adapter } from '@/api/__generated__'
import { useCreateProtocolAdapter } from '@/api/hooks/useProtocolAdapters/useCreateProtocolAdapter'
import { useWizardStore } from '@/modules/Workspace/hooks/useWizardStore'
import { removeGhostNodes, removeGhostEdges } from '../utils/ghostNodeFactory'

interface AdapterConfig extends Record<string, unknown> {
  id: string
}

export const useCompleteAdapterWizard = () => {
  const { t } = useTranslation()
  const toast = useToast()
  const { getNodes, setNodes, getEdges, setEdges } = useReactFlow()
  const { mutateAsync: createAdapter } = useCreateProtocolAdapter()
  const [isCompleting, setIsCompleting] = useState(false)

  const completeWizard = async () => {
    setIsCompleting(true)

    try {
      // Read directly from store to avoid stale closure
      const { configurationData } = useWizardStore.getState()
      const { protocolId, adapterConfig } = configurationData

      if (!protocolId || !adapterConfig) {
        const missing = []
        if (!protocolId) missing.push('protocol type')
        if (!adapterConfig) missing.push('adapter configuration')
        throw new Error(`Missing configuration data: ${missing.join(', ')}`)
      }

      // 1. Create adapter via API
      // Wrap config in proper Adapter structure
      const config = adapterConfig as AdapterConfig
      const adapterId = config.id

      await createAdapter({
        adapterType: protocolId as string,
        requestBody: {
          id: adapterId,
          type: protocolId as string,
          config: adapterConfig,
        } as Adapter,
      })

      // 2. TRANSITION SEQUENCE: Ghost → Real nodes
      // ==========================================
      // Current implementation uses time-based delays to create a smooth visual transition.
      // This works well but could be improved with event-based triggers.
      //
      // CURRENT APPROACH:
      // - API completes → Ghost fades out (500ms) → Wait 600ms → Remove ghost → Real nodes appear
      // - Total perceived transition: ~600ms
      //
      // POTENTIAL IMPROVEMENTS:
      // - Listen to React Query cache update event instead of fixed delay
      // - Wait for actual DOM render of real nodes before removing ghosts
      // - Use React Flow's onNodesChange to detect new nodes appearing
      // - Coordinate with useGetFlowElements refresh cycle
      // - Add loading skeleton/shimmer during transition
      // - Morph ghost directly into real node (position already matches)
      //
      // CONSTRAINTS TO CONSIDER:
      // - React Query invalidation timing (when does refetch complete?)
      // - useGetFlowElements useEffect dependencies and execution order
      // - React Flow render cycle (when are new nodes actually painted?)
      // - Browser paint timing (requestAnimationFrame considerations)
      // - User perception (anything under 300ms feels instant, 300-1000ms needs feedback)
      //
      // For now, the time-based approach provides a reliable, smooth transition.
      // Future iteration could make this more robust with proper event coordination.

      const nodes = getNodes()

      // Fade out ghost nodes (blue glow dims to 30% opacity over 500ms)
      const nodesWithFade = nodes.map((node) => {
        if (node.data?.isGhost) {
          return {
            ...node,
            style: {
              ...node.style,
              opacity: 0.3,
              transition: 'opacity 0.5s ease-out',
            },
          }
        }
        return node
      })

      setNodes(nodesWithFade)

      // 3. Wait for fade animation to complete and real nodes to appear
      // The 600ms delay allows:
      // - Ghost fade animation to complete (500ms)
      // - React Query to invalidate and refetch (variable)
      // - useGetFlowElements to process new data (variable)
      // - React Flow to render new nodes (variable)
      // TODO: Replace with event-based trigger when React Query cache updates
      await new Promise((resolve) => setTimeout(resolve, 600))

      // 4. Remove ghost nodes and edges
      // By this point, real nodes should be visible at the same position
      const realNodes = removeGhostNodes(getNodes())
      const realEdges = removeGhostEdges(getEdges())

      setNodes(realNodes)
      setEdges(realEdges)

      // 5. Complete wizard (clears store and resets state)
      const { actions } = useWizardStore.getState()
      actions.completeWizard()

      // 6. Highlight new adapter nodes briefly (visual feedback for successful creation)
      // Green glow appears after 100ms to give React Flow time to fully render the new nodes
      // TODO: Could use React Flow's onNodesChange or IntersectionObserver to detect when nodes are visible
      setTimeout(() => {
        const newAdapterNodeId = `ADAPTER_NODE@${adapterId}`
        const newDeviceNodeId = `DEVICE_NODE@${newAdapterNodeId}`

        // Apply green glow (success color) to new nodes
        const nodesWithHighlight = getNodes().map((node) => {
          if (node.id === newAdapterNodeId || node.id === newDeviceNodeId) {
            return {
              ...node,
              style: {
                ...node.style,
                boxShadow: '0 0 0 4px rgba(72, 187, 120, 0.6), 0 0 20px rgba(72, 187, 120, 0.4)',
                transition: 'box-shadow 0.3s ease-in',
              },
            }
          }
          return node
        })

        setNodes(nodesWithHighlight)

        // Remove highlight after 2 seconds (long enough to notice, short enough not to annoy)
        setTimeout(() => {
          const nodesWithoutHighlight = getNodes().map((node) => {
            if (node.id === newAdapterNodeId || node.id === newDeviceNodeId) {
              return {
                ...node,
                style: {
                  ...node.style,
                  boxShadow: undefined,
                  transition: 'box-shadow 0.5s ease-out',
                },
              }
            }
            return node
          })
          setNodes(nodesWithoutHighlight)
        }, 2000)
      }, 100)

      // 7. Show success feedback
      const adapterName = config.id || 'Adapter'
      toast({
        title: t('protocolAdapter.action.create'),
        description: t('workspace.wizard.success.adapterCreated', { name: adapterName }),
        status: 'success',
        duration: 5000,
        isClosable: true,
      })

      setIsCompleting(false)
      return true
    } catch (error) {
      setIsCompleting(false)

      // Set error in wizard store
      const { actions } = useWizardStore.getState()
      actions.setError((error as Error).message || 'Failed to create adapter')

      // Show error toast
      toast({
        title: t('protocolAdapter.error.title'),
        description: (error as Error).message || t('protocolAdapter.error.loading'),
        status: 'error',
        duration: 7000,
        isClosable: true,
      })

      return false
    }
  }

  return {
    completeWizard,
    isCompleting,
  }
}
