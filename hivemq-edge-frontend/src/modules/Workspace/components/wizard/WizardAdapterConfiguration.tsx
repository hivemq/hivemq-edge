/**
 * Wizard Adapter Configuration
 *
 * Orchestrates the adapter creation process within the wizard:
 * - Step 1 (currentStep=1): Protocol type selection
 * - Step 2 (currentStep=2): Adapter configuration form
 *
 * Reuses existing components from ProtocolAdapters module.
 */

import type { FC } from 'react'
import { useState } from 'react'

import type { Adapter } from '@/api/__generated__'
import { useWizardState, useWizardActions, useWizardConfiguration } from '@/modules/Workspace/hooks/useWizardStore'
import { EntityType } from './types'
import { useCompleteAdapterWizard } from './hooks/useCompleteAdapterWizard'

import WizardProtocolSelector from './steps/WizardProtocolSelector.tsx'
import WizardAdapterForm from './steps/WizardAdapterForm.tsx'

/**
 * Main wizard configuration component for adapters
 * Handles step transitions and data flow
 */
const WizardAdapterConfiguration: FC = () => {
  const { isActive, entityType, currentStep } = useWizardState()
  const { nextStep, previousStep } = useWizardActions()
  const { configurationData, updateConfiguration } = useWizardConfiguration()
  const { completeWizard } = useCompleteAdapterWizard()

  const [selectedProtocolId, setSelectedProtocolId] = useState<string | undefined>(
    configurationData.protocolId as string | undefined
  )

  // Only render for adapter wizard
  if (!isActive || entityType !== EntityType.ADAPTER) {
    return null
  }

  // Step 1: Protocol Selection (currentStep = 1)
  if (currentStep === 1) {
    const handleProtocolSelect = (protocolId: string | undefined) => {
      if (!protocolId) return

      setSelectedProtocolId(protocolId)

      // Save to wizard store
      updateConfiguration({
        protocolId,
      })

      // Move to next step
      nextStep()
    }

    return <WizardProtocolSelector onSelect={handleProtocolSelect} />
  }

  // Step 2: Adapter Configuration (currentStep = 2)
  if (currentStep === 2) {
    const handleFormSubmit = async (adapterData: Adapter) => {
      // Save adapter configuration to wizard store first
      updateConfiguration({
        protocolId: selectedProtocolId,
        adapterConfig: adapterData,
      })

      // Wait for next tick to ensure state is updated
      await new Promise((resolve) => setTimeout(resolve, 0))

      // Trigger wizard completion with API call
      await completeWizard()
    }

    const handleBack = () => {
      previousStep()
    }

    return <WizardAdapterForm protocolId={selectedProtocolId} onSubmit={handleFormSubmit} onBack={handleBack} />
  }

  // Should not reach here, but return null for safety
  return null
}

export default WizardAdapterConfiguration
