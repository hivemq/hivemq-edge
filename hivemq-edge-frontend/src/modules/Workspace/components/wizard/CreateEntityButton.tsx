/**
 * Create Entity Button
 *
 * Dropdown button in CanvasToolbar that allows users to start the wizard
 * for creating entities or adding integration points.
 */

import type { FC } from 'react'
import { useTranslation } from 'react-i18next'
import {
  Button,
  Menu,
  MenuButton,
  MenuList,
  MenuItem,
  MenuGroup,
  MenuDivider,
  Icon,
  HStack,
  Text,
  Portal,
} from '@chakra-ui/react'
import { ChevronDownIcon } from '@chakra-ui/icons'
import { LuPlus } from 'react-icons/lu'

import { Capability } from '@/api/__generated__'
import { useGetCapability } from '@/api/hooks/useFrontendServices/useGetCapability'
import { useWizardState, useWizardActions } from '@/modules/Workspace/hooks/useWizardStore'
import { getEntityWizardTypes, getIntegrationWizardTypes, getWizardIcon } from './utils/wizardMetadata'
import type { WizardType } from './types'

/**
 * Button that opens a menu to start different wizard types
 */
const CreateEntityButton: FC = () => {
  const { t } = useTranslation()
  const { isActive } = useWizardState()
  const { startWizard } = useWizardActions()
  const { data: hasPulse } = useGetCapability(Capability.id.PULSE_ASSET_MANAGEMENT)

  const entityTypes = getEntityWizardTypes()
  const integrationTypes = getIntegrationWizardTypes()

  // Track which wizards are implemented
  // Note: Combiner, Asset Mapper, and Group enabled after Subtask 9 (Selection System)
  const implementedWizards = new Set(['ADAPTER', 'BRIDGE', 'COMBINER', 'ASSET_MAPPER'])

  const handleSelectWizard = (type: WizardType) => {
    startWizard(type)
  }

  const isWizardImplemented = (type: WizardType): boolean => {
    return implementedWizards.has(type)
  }

  const isWizardAvailable = (type: WizardType): boolean => {
    // Asset Mapper requires Pulse capability
    if (type === 'ASSET_MAPPER' && !hasPulse) {
      return false
    }
    return isWizardImplemented(type)
  }

  return (
    <Menu placement="bottom-start">
      <MenuButton
        as={Button}
        variant="outline"
        size="sm"
        leftIcon={<Icon as={LuPlus} />}
        rightIcon={<ChevronDownIcon />}
        aria-label={t('workspace.wizard.trigger.buttonAriaLabel')}
        data-testid="create-entity-button"
        isDisabled={isActive}
        title={isActive ? t('workspace.wizard.trigger.disabledTooltip') : undefined}
      >
        {t('workspace.wizard.trigger.buttonLabel')}
      </MenuButton>

      <Portal>
        <MenuList maxH="400px" overflowY="auto" role="menu" aria-label={t('workspace.wizard.trigger.menuTitle')}>
          <MenuGroup title={t('workspace.wizard.category.entities')}>
            {entityTypes.map((type) => {
              const IconComponent = getWizardIcon(type)
              const isAvailable = isWizardAvailable(type)
              const isImplemented = isWizardImplemented(type)
              const isPulseRequired = type === 'ASSET_MAPPER' && !hasPulse

              return (
                <MenuItem
                  key={type}
                  icon={<Icon as={IconComponent} boxSize={4} />}
                  onClick={() => handleSelectWizard(type)}
                  data-testid={`wizard-option-${type}`}
                  isDisabled={!isAvailable}
                  opacity={isAvailable ? 1 : 0.5}
                  cursor={isAvailable ? 'pointer' : 'not-allowed'}
                  title={
                    isPulseRequired
                      ? t('workspace.wizard.assetMapper.requiresPulse')
                      : !isImplemented
                        ? t('workspace.wizard.assetMapper.comingSoon')
                        : undefined
                  }
                >
                  <HStack spacing={2} align="center">
                    <Text>{t('workspace.wizard.entityType.name', { context: type })}</Text>
                    {isPulseRequired && (
                      <Text fontSize="xs" color="gray.500">
                        {t('workspace.wizard.assetMapper.requiresPulse')}
                      </Text>
                    )}
                    {!isImplemented && !isPulseRequired && (
                      <Text fontSize="xs" color="gray.500">
                        {t('workspace.wizard.assetMapper.comingSoon')}
                      </Text>
                    )}
                  </HStack>
                </MenuItem>
              )
            })}
          </MenuGroup>

          <MenuDivider />

          <MenuGroup title={t('workspace.wizard.category.integrationPoints')}>
            {integrationTypes.map((type) => {
              const IconComponent = getWizardIcon(type)
              const isAvailable = isWizardAvailable(type)
              const isImplemented = isWizardImplemented(type)

              return (
                <MenuItem
                  key={type}
                  icon={<Icon as={IconComponent} boxSize={4} />}
                  onClick={() => handleSelectWizard(type)}
                  data-testid={`wizard-option-${type}`}
                  isDisabled={!isAvailable}
                  opacity={isAvailable ? 1 : 0.5}
                  cursor={isAvailable ? 'pointer' : 'not-allowed'}
                  title={!isImplemented ? 'Coming soon' : undefined}
                >
                  <HStack spacing={2} align="center">
                    <Text>{t('workspace.wizard.entityType.name', { context: type })}</Text>
                    {!isImplemented && (
                      <Text fontSize="xs" color="gray.500">
                        (Coming soon)
                      </Text>
                    )}
                  </HStack>
                </MenuItem>
              )
            })}
          </MenuGroup>
        </MenuList>
      </Portal>
    </Menu>
  )
}

export default CreateEntityButton
