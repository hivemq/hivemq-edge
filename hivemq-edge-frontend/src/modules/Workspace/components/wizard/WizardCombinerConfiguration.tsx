/**
 * Wizard Combiner Configuration
 *
 * Wrapper component for CombinerMappingManager in wizard mode.
 * Handles API integration for creating combiner during wizard flow.
 */

import type { FC } from 'react'
import { useTranslation } from 'react-i18next'
import { useToast } from '@chakra-ui/react'

import type { Combiner } from '@/api/__generated__'
import { useCreateCombiner } from '@/api/hooks/useCombiners/useCreateCombiner'
import { useCreateAssetMapper } from '@/api/hooks/useAssetMapper/useCreateAssetMapper'
import { BASE_TOAST_OPTION } from '@/hooks/useEdgeToast/toast-utils'
import CombinerMappingManager from '@/modules/Mappings/CombinerMappingManager'
import { useWizardState, useWizardActions } from '@/modules/Workspace/hooks/useWizardStore'

const WizardCombinerConfiguration: FC = () => {
  const { t } = useTranslation()
  const { entityType, selectedNodeIds } = useWizardState()
  const { completeWizard, previousStep } = useWizardActions()

  // Use different API hooks based on entity type
  const createCombiner = useCreateCombiner()
  const createAssetMapper = useCreateAssetMapper()
  const toast = useToast(BASE_TOAST_OPTION)

  // Asset Mapper and Combiner use different APIs but same schema
  const isAssetMapper = entityType === 'ASSET_MAPPER'
  const entityKey = isAssetMapper ? 'assetMapper' : 'combiner'

  const handleComplete = async (combinerData: Combiner) => {
    // Create via appropriate API endpoint
    try {
      if (isAssetMapper) {
        await createAssetMapper.mutateAsync({ requestBody: combinerData })
      } else {
        await createCombiner.mutateAsync({ requestBody: combinerData })
      }

      // Success - complete wizard
      await completeWizard()

      toast({
        title: t(`workspace.wizard.${entityKey}.success.title`),
        description: t(`workspace.wizard.${entityKey}.success.message`, { name: combinerData.name }),
        status: 'success',
      })
    } catch (error) {
      // Error - keep wizard open, show error
      toast({
        title: t(`workspace.wizard.${entityKey}.error.title`),
        description: error instanceof Error ? error.message : t(`workspace.wizard.${entityKey}.error.message`),
        status: 'error',
      })

      // Re-throw to prevent wizard from closing
      throw error
    }
  }

  const handleCancel = () => {
    // Return to previous step (selection)
    previousStep()
  }

  return (
    <CombinerMappingManager
      wizardContext={{
        isWizardMode: true,
        selectedNodeIds,
        onComplete: handleComplete,
        onCancel: handleCancel,
      }}
    />
  )
}

export default WizardCombinerConfiguration
