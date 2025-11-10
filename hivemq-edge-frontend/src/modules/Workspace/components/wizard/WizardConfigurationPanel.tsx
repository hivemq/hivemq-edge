/**
 * Wizard Configuration Panel
 *
 * Side panel that appears during wizard configuration steps.
 * Shows the appropriate configuration UI based on entity type and current step.
 */

import type { FC } from 'react'
import { Drawer, DrawerContent, DrawerOverlay } from '@chakra-ui/react'
import { useTranslation } from 'react-i18next'

import { useWizardState } from '@/modules/Workspace/hooks/useWizardStore'
import { EntityType } from './types'
import { getWizardStep } from './utils/wizardMetadata'

import WizardAdapterConfiguration from './WizardAdapterConfiguration.tsx'

/**
 * Main configuration panel for wizard
 * Renders appropriate configuration UI based on wizard state
 */
const WizardConfigurationPanel: FC = () => {
  const { t } = useTranslation()
  const { isActive, entityType, currentStep } = useWizardState()

  // Only show panel when wizard is active
  if (!isActive || !entityType) {
    return null
  }

  // Get step configuration to check if this step requires configuration
  const stepConfig = getWizardStep(entityType, currentStep)
  const showsConfigurationPanel = stepConfig?.requiresConfiguration || stepConfig?.requiresSelection

  // Don't show panel on steps that don't need it (e.g., ghost preview step)
  if (!showsConfigurationPanel) {
    return null
  }

  const renderConfigurationContent = () => {
    switch (entityType) {
      case EntityType.ADAPTER:
        return <WizardAdapterConfiguration />

      case EntityType.BRIDGE:
        // TODO: Implement bridge configuration
        return <div>Bridge configuration coming soon</div>

      case EntityType.COMBINER:
        // TODO: Implement combiner configuration
        return <div>Combiner configuration coming soon</div>

      case EntityType.ASSET_MAPPER:
        // TODO: Implement asset mapper configuration
        return <div>Asset Mapper configuration coming soon</div>

      case EntityType.GROUP:
        // TODO: Implement group configuration
        return <div>Group configuration coming soon</div>

      default:
        return null
    }
  }

  return (
    <Drawer
      isOpen={true}
      placement="right"
      onClose={() => {
        // Don't allow closing via overlay/escape during wizard
        // User must use Cancel button in progress bar
      }}
      closeOnOverlayClick={false}
      closeOnEsc={false}
      size="lg"
      variant="hivemq"
    >
      <DrawerOverlay />
      <DrawerContent aria-label={t('workspace.wizard.configPanel.ariaLabel')} data-testid="wizard-configuration-panel">
        {renderConfigurationContent()}
      </DrawerContent>
    </Drawer>
  )
}

export default WizardConfigurationPanel
