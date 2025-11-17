import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useToast } from '@chakra-ui/react'
import { useReactFlow } from '@xyflow/react'

import type { Adapter } from '@/api/__generated__'
import { useCreateProtocolAdapter } from '@/api/hooks/useProtocolAdapters/useCreateProtocolAdapter'
import { useWizardStore } from '@/modules/Workspace/hooks/useWizardStore'
import { useCompleteUtilities } from '@/modules/Workspace/components/wizard/hooks/useCompleteUtilities.ts'

interface AdapterConfig extends Record<string, unknown> {
  id: string
}

export const useCompleteAdapterWizard = () => {
  const { t } = useTranslation()
  const toast = useToast()
  const { getNodes, setNodes } = useReactFlow()
  const { mutateAsync: createAdapter } = useCreateProtocolAdapter()
  const [isCompleting, setIsCompleting] = useState(false)
  const { handleTransitionSequence } = useCompleteUtilities()

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
      await handleTransitionSequence()

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
