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

  const entityTypes = getEntityWizardTypes()
  const integrationTypes = getIntegrationWizardTypes()

  const handleSelectWizard = (type: WizardType) => {
    startWizard(type)
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
          {/* Entity Wizards Section */}
          <MenuGroup title={t('workspace.wizard.category.entities')}>
            {entityTypes.map((type) => {
              const IconComponent = getWizardIcon(type)
              return (
                <MenuItem
                  key={type}
                  icon={<Icon as={IconComponent} boxSize={4} />}
                  onClick={() => handleSelectWizard(type)}
                  data-testid={`wizard-option-${type}`}
                >
                  <HStack spacing={2} align="center">
                    <Text>{t('workspace.wizard.entityType.name', { context: type })}</Text>
                  </HStack>
                </MenuItem>
              )
            })}
          </MenuGroup>

          <MenuDivider />

          {/* Integration Point Wizards Section */}
          <MenuGroup title={t('workspace.wizard.category.integrationPoints')}>
            {integrationTypes.map((type) => {
              const IconComponent = getWizardIcon(type)
              return (
                <MenuItem
                  key={type}
                  icon={<Icon as={IconComponent} boxSize={4} />}
                  onClick={() => handleSelectWizard(type)}
                  data-testid={`wizard-option-${type}`}
                >
                  <HStack spacing={2} align="center">
                    <Text>{t('workspace.wizard.entityType.name', { context: type })}</Text>
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
