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

import WizardAdapterConfiguration from './WizardAdapterConfiguration'
import WizardBridgeConfiguration from './WizardBridgeConfiguration'
import WizardCombinerConfiguration from './WizardCombinerConfiguration'
import WizardGroupConfiguration from './WizardGroupConfiguration'

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

  // Selection steps use floating Panel, not Drawer
  // Don't show Drawer for selection steps
  if (stepConfig?.requiresSelection) {
    return null
  }

  const showsConfigurationPanel = stepConfig?.requiresConfiguration

  // Don't show panel on steps that don't need it (e.g., ghost preview step)
  if (!showsConfigurationPanel) {
    return null
  }

  const renderConfigurationContent = () => {
    // Route to entity-specific configuration
    switch (entityType) {
      case EntityType.ADAPTER:
        return <WizardAdapterConfiguration />

      case EntityType.BRIDGE:
        return <WizardBridgeConfiguration />

      case EntityType.COMBINER:
        // Combiner has its own Drawer - render directly without wrapper
        return <WizardCombinerConfiguration />

      case EntityType.ASSET_MAPPER:
        // Asset Mapper uses same schema as Combiner, just with Pulse Agent auto-included
        // Reuse Combiner configuration component
        return <WizardCombinerConfiguration />

      case EntityType.GROUP:
        return <WizardGroupConfiguration />

      default:
        return null
    }
  }

  // Some components (like CombinerMappingManager) have their own Drawer
  // Check if we need to wrap in a Drawer
  // Combiner and Asset Mapper have their own Drawer
  const needsDrawerWrapper = entityType !== EntityType.COMBINER && entityType !== EntityType.ASSET_MAPPER

  if (needsDrawerWrapper) {
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
        <DrawerContent
          aria-label={t('workspace.wizard.configPanel.ariaLabel')}
          data-testid="wizard-configuration-panel"
        >
          {renderConfigurationContent()}
        </DrawerContent>
      </Drawer>
    )
  }

  // Render component directly (it has its own Drawer)
  return <>{renderConfigurationContent()}</>
}

export default WizardConfigurationPanel
