/**
 * useCompleteCombinerWizard Hook
 *
 * Handles the final step of the combiner/asset mapper wizard:
 * - Creates combiner via API
 * - Removes ghost nodes/edges
 * - Shows success/error feedback
 * - Completes wizard
 */

import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useToast } from '@chakra-ui/react'
import { useReactFlow } from '@xyflow/react'

import type { Combiner } from '@/api/__generated__'
import { useCreateCombiner } from '@/api/hooks/useCombiners/useCreateCombiner'
import { useCreateAssetMapper } from '@/api/hooks/useAssetMapper/useCreateAssetMapper'
import { BASE_TOAST_OPTION } from '@/hooks/useEdgeToast/toast-utils'
import { useWizardStore } from '@/modules/Workspace/hooks/useWizardStore'
import { removeGhostNodes, removeGhostEdges } from '../utils/ghostNodeFactory'

interface UseCompleteCombinerWizardOptions {
  isAssetMapper: boolean
}

export const useCompleteCombinerWizard = ({ isAssetMapper }: UseCompleteCombinerWizardOptions) => {
  const { t } = useTranslation()
  const toast = useToast(BASE_TOAST_OPTION)
  const { getNodes, setNodes, getEdges, setEdges } = useReactFlow()
  const { mutateAsync: createCombiner } = useCreateCombiner()
  const { mutateAsync: createAssetMapper } = useCreateAssetMapper()
  const [isCompleting, setIsCompleting] = useState(false)

  const entityKey = isAssetMapper ? 'assetMapper' : 'combiner'

  const completeWizard = async (combinerData: Combiner) => {
    setIsCompleting(true)

    try {
      // 1. Create combiner/asset mapper via API
      if (isAssetMapper) {
        await createAssetMapper({ requestBody: combinerData })
      } else {
        await createCombiner({ requestBody: combinerData })
      }

      // 2. TRANSITION SEQUENCE: Ghost → Real nodes
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
      await new Promise((resolve) => setTimeout(resolve, 600))

      // 4. Remove ghost nodes and edges
      const realNodes = removeGhostNodes(getNodes())
      const realEdges = removeGhostEdges(getEdges())

      setNodes(realNodes)
      setEdges(realEdges)

      // 5. Reset wizard state (we handle API and validation in this hook)
      const { actions } = useWizardStore.getState()
      actions.cancelWizard() // Direct reset without validation check

      // 6. Highlight new combiner node briefly (green glow for visual feedback)
      setTimeout(() => {
        const newCombinerNodeId = `COMBINER_NODE@${combinerData.id}`

        // Apply green glow (success color) to new node
        const nodesWithHighlight = getNodes().map((node) => {
          if (node.id === newCombinerNodeId) {
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

        // Remove highlight after 2 seconds
        setTimeout(() => {
          const nodesWithoutHighlight = getNodes().map((node) => {
            if (node.id === newCombinerNodeId) {
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
      toast({
        title: t(`workspace.wizard.${entityKey}.success.title`),
        description: t(`workspace.wizard.${entityKey}.success.message`, { name: combinerData.name }),
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
      actions.setError((error as Error).message || `Failed to create ${entityKey}`)

      // Show error toast
      toast({
        title: t(`workspace.wizard.${entityKey}.error.title`),
        description: (error as Error).message || t(`workspace.wizard.${entityKey}.error.message`),
        status: 'error',
        duration: 7000,
        isClosable: true,
      })

      // Re-throw to prevent wizard from closing
      throw error
    }
  }

  return {
    completeWizard,
    isCompleting,
  }
}
