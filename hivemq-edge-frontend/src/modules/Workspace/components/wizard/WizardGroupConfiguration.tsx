/**
 * Wizard Group Configuration
 *
 * Manages the configuration steps for the Group wizard.
 * Routes between different steps based on current wizard state.
 */

import type { FC } from 'react'

import { useWizardState, useWizardActions, useWizardConfiguration } from '@/modules/Workspace/hooks/useWizardStore'
import { useCompleteGroupWizard } from './hooks/useCompleteGroupWizard'
import WizardGroupForm from './steps/WizardGroupForm'

const WizardGroupConfiguration: FC = () => {
  const { currentStep } = useWizardState()
  const { previousStep } = useWizardActions()
  const { updateConfiguration } = useWizardConfiguration()
  const { completeWizard } = useCompleteGroupWizard()

  // Step 0: Selection (no configuration panel)
  // Handled by WizardSelectionPanel
  // Ghost group visible during selection

  // Step 1: Group Configuration
  if (currentStep === 1) {
    const handleFormSubmit = (groupData: { title: string; colorScheme: string }) => {
      // Save group configuration to wizard store
      updateConfiguration({
        groupConfig: groupData,
      })

      // Complete wizard (will be handled by completion logic in Subtask 6)
      completeWizard()
    }

    const handleBack = () => {
      previousStep()
    }

    return <WizardGroupForm onSubmit={handleFormSubmit} onBack={handleBack} />
  }

  // Should not reach here, but return null for safety
  return null
}

export default WizardGroupConfiguration
