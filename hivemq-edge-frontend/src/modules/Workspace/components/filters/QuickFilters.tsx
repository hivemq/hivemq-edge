import ConfirmationDialog from '@/components/Modal/ConfirmationDialog.tsx'
import ConfigurationSelector from '@/modules/Workspace/components/filters/ConfigurationSelector.tsx'
import { ChevronDownIcon } from '@chakra-ui/icons'
import type { FC } from 'react'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useLocalStorage } from '@uidotdev/usehooks'
import {
  Card,
  CardBody,
  CardFooter,
  CardHeader,
  FormControl,
  HStack,
  IconButton,
  Menu,
  MenuButton,
  MenuItem,
  MenuList,
  Switch,
  Text,
  useDisclosure,
  VStack,
} from '@chakra-ui/react'

import type { FilterConfigurationOption } from '@/modules/Workspace/components/filters/types.ts'
import { KEY_FILTER_CONFIGURATIONS } from '@/modules/Workspace/components/filters/types.ts'
import { ConfigurationSave } from '@/modules/Workspace/components/filters/index.ts'

interface QuickFilterProps {
  id?: string
}

const QuickFilters: FC<QuickFilterProps> = () => {
  const { t } = useTranslation()
  const [configurations, setConfigurations] = useLocalStorage<FilterConfigurationOption[]>(
    KEY_FILTER_CONFIGURATIONS,
    []
  )
  const [selectedFilter, setSelectedFilter] = useState<string>()
  const { isOpen: isConfirmDeleteOpen, onOpen: onConfirmDeleteOpen, onClose: onConfirmDeleteClose } = useDisclosure()

  const handleOpenConfirmation = (id: string) => {
    setSelectedFilter(id)
    onConfirmDeleteOpen()
  }

  const handleConfigDelete = () => {
    if (!selectedFilter) return
    setConfigurations((old) => old.filter((option) => option.label !== selectedFilter))
    handleCloseConfirmation()
  }

  const handleCloseConfirmation = () => {
    onConfirmDeleteClose()
    setSelectedFilter(undefined)
  }

  return (
    <>
      <Card
        size="sm"
        alignContent="space-between"
        width="-webkit-fill-available"
        data-testid="workspace-quick-filters-container"
      >
        <CardHeader>
          <HStack alignItems="flex-start">
            <Text>{t('workspace.searchToolbox.quickFilters.label')}</Text>
          </HStack>
        </CardHeader>
        <CardBody as={VStack} gap={3}>
          {configurations.map((config) => (
            <FormControl key={config.label} as={HStack}>
              <Switch
                flex={1}
                id={`workspace-filter-quick-${config.label}`}
                // onChange={(e) => onChange?.(e.target.checked)}
                // isChecked={isActive}
              >
                {config.label}
              </Switch>
              <Menu id="asset-actions">
                <MenuButton
                  variant="outline"
                  size="xs"
                  as={IconButton}
                  icon={<ChevronDownIcon />}
                  aria-label={t('pulse.assets.actions.aria-label')}
                />
                <MenuList>
                  <MenuItem data-testid="assets-action-view">
                    {t('workspace.searchToolbox.quickFilters.action.edit')}
                  </MenuItem>
                  <MenuItem data-testid="assets-action-view" onClick={() => handleOpenConfirmation(config.label)}>
                    {t('workspace.searchToolbox.quickFilters.action.delete')}
                  </MenuItem>
                </MenuList>
              </Menu>
            </FormControl>
          ))}
        </CardBody>
        <CardFooter>
          <ConfigurationSave isFilterActive={isFilterActive} configurations={configurations} />
        </CardFooter>
      </Card>
      <ConfirmationDialog
        isOpen={isConfirmDeleteOpen}
        onClose={handleCloseConfirmation}
        onSubmit={handleConfigDelete}
        message={t('workspace.searchToolbox.quickFilters.delete.message')}
        header={t('workspace.searchToolbox.quickFilters.delete.header')}
      />
    </>
  )
}

export default QuickFilters
