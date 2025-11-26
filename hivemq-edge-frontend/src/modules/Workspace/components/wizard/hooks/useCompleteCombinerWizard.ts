import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useToast } from '@chakra-ui/react'
import { useReactFlow } from '@xyflow/react'

import type { Combiner } from '@/api/__generated__'
import { useCreateCombiner } from '@/api/hooks/useCombiners/useCreateCombiner'
import { useCreateAssetMapper } from '@/api/hooks/useAssetMapper/useCreateAssetMapper'
import { BASE_TOAST_OPTION, DEFAULT_TOAST_OPTION } from '@/hooks/useEdgeToast/toast-utils'
import { useWizardStore } from '@/modules/Workspace/hooks/useWizardStore'
import { useCompleteUtilities } from '@/modules/Workspace/components/wizard/hooks/useCompleteUtilities.ts'
import { GHOST_SUCCESS_BOX_SHADOW_TRANSITION, GHOST_SUCCESS_TRANSITION, GHOST_SUCCESS_SHADOW } from '../utils/styles'

interface UseCompleteCombinerWizardOptions {
  isAssetMapper: boolean
}

export const useCompleteCombinerWizard = ({ isAssetMapper }: UseCompleteCombinerWizardOptions) => {
  const { t } = useTranslation()
  const toast = useToast(BASE_TOAST_OPTION)
  const { getNodes, setNodes } = useReactFlow()
  const { mutateAsync: createCombiner } = useCreateCombiner()
  const { handleTransitionSequence } = useCompleteUtilities()
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

      // // 2. TRANSITION SEQUENCE: Ghost → Real nodes
      await handleTransitionSequence()

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
                boxShadow: GHOST_SUCCESS_SHADOW,
                transition: GHOST_SUCCESS_TRANSITION,
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
                  transition: GHOST_SUCCESS_BOX_SHADOW_TRANSITION,
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
        ...DEFAULT_TOAST_OPTION,
        title: t(`workspace.wizard.${entityKey}.success.title`),
        description: t(`workspace.wizard.${entityKey}.success.message`, { name: combinerData.name }),
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
        ...DEFAULT_TOAST_OPTION,
        title: t(`workspace.wizard.${entityKey}.error.title`),
        description: (error as Error).message || t(`workspace.wizard.${entityKey}.error.message`),
        status: 'error',
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
