import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useToast } from '@chakra-ui/react'
import { useReactFlow } from '@xyflow/react'

import type { Bridge } from '@/api/__generated__'
import { useCreateBridge } from '@/api/hooks/useGetBridges/useCreateBridge'
import { useWizardStore } from '@/modules/Workspace/hooks/useWizardStore'
import { useCompleteUtilities } from '@/modules/Workspace/components/wizard/hooks/useCompleteUtilities.ts'

interface BridgeConfig extends Record<string, unknown> {
  id: string
}

export const useCompleteBridgeWizard = () => {
  const { t } = useTranslation()
  const toast = useToast()
  const { getNodes, setNodes } = useReactFlow()
  const { mutateAsync: createBridge } = useCreateBridge()
  const [isCompleting, setIsCompleting] = useState(false)
  const { handleTransitionSequence } = useCompleteUtilities()

  const completeWizard = async () => {
    setIsCompleting(true)

    try {
      // Read directly from store to avoid stale closure
      const { configurationData } = useWizardStore.getState()
      const { bridgeConfig } = configurationData

      if (!bridgeConfig) {
        const missing = []
        if (!bridgeConfig) missing.push('bridge configuration')
        throw new Error(`Missing configuration data: ${missing.join(', ')}`)
      }

      // 1. Create bridge via API
      const config = bridgeConfig as BridgeConfig
      const bridgeId = config.id

      await createBridge(bridgeConfig as Bridge)

      // // 2. TRANSITION SEQUENCE: Ghost → Real nodes
      await handleTransitionSequence()

      // 6. Highlight new bridge nodes briefly (green glow for visual feedback)
      setTimeout(() => {
        const newBridgeNodeId = `BRIDGE_NODE@${bridgeId}`
        const newHostNodeId = `HOST_NODE@BRIDGE_NODE@${bridgeId}`

        // Apply green glow (success color) to new nodes
        const nodesWithHighlight = getNodes().map((node) => {
          if (node.id === newBridgeNodeId || node.id === newHostNodeId) {
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
            if (node.id === newBridgeNodeId || node.id === newHostNodeId) {
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
      const bridgeName = config.id || 'Bridge'
      toast({
        title: t('protocolAdapter.action.create'),
        description: t('workspace.wizard.success.bridgeCreated', { name: bridgeName }),
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
      actions.setError((error as Error).message || 'Failed to create bridge')

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
