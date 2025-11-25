/**
 * Wizard Bridge Configuration
 *
 * Manages the configuration steps for the Bridge wizard.
 * Routes between different steps based on current wizard state.
 */

import type { FC } from 'react'

import type { Bridge } from '@/api/__generated__'
import { useWizardState, useWizardActions, useWizardConfiguration } from '@/modules/Workspace/hooks/useWizardStore'
import { useCompleteBridgeWizard } from './hooks/useCompleteBridgeWizard'

import WizardBridgeForm from './steps/WizardBridgeForm'

const WizardBridgeConfiguration: FC = () => {
  const { currentStep } = useWizardState()
  const { previousStep } = useWizardActions()
  const { updateConfiguration } = useWizardConfiguration()
  const { completeWizard } = useCompleteBridgeWizard()

  // Step 0: Ghost Preview (no configuration panel)
  // Handled by GhostNodeRenderer

  // Step 1: Bridge Configuration
  if (currentStep === 1) {
    const handleFormSubmit = async (bridgeData: Bridge) => {
      // Save bridge configuration to wizard store
      updateConfiguration({
        bridgeConfig: bridgeData,
      })

      // Wait for next tick to ensure state is updated
      await new Promise((resolve) => setTimeout(resolve, 0))

      // Trigger wizard completion with API call
      await completeWizard()
    }

    const handleBack = () => {
      previousStep()
    }

    return <WizardBridgeForm onSubmit={handleFormSubmit} onBack={handleBack} />
  }

  // Should not reach here, but return null for safety
  return null
}

export default WizardBridgeConfiguration
