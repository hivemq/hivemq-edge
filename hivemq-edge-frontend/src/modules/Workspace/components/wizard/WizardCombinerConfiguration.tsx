import type { FC } from 'react'
import type { Combiner } from '@/api/__generated__'
import CombinerMappingManager from '@/modules/Mappings/CombinerMappingManager'
import { EntityType } from '@/modules/Workspace/components/wizard/types.ts'
import { useWizardActions, useWizardState } from '@/modules/Workspace/hooks/useWizardStore'
import { useCompleteCombinerWizard } from './hooks/useCompleteCombinerWizard'

const WizardCombinerConfiguration: FC = () => {
  const { entityType, selectedNodeIds } = useWizardState()
  const { previousStep } = useWizardActions()

  // Asset Mapper and Combiner use different APIs but same schema
  const isAssetMapper = entityType === EntityType.ASSET_MAPPER

  // Use completion hook (like bridge wizard)
  const { completeWizard } = useCompleteCombinerWizard({ isAssetMapper })

  const handleComplete = async (combinerData: Combiner) => {
    // Hook handles everything: API call, ghost removal, wizard completion, toast
    await completeWizard(combinerData)
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
