import type { FC } from 'react'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useLocalStorage } from '@uidotdev/usehooks'
import type { SingleValue } from 'chakra-react-select'
import {
  Card,
  CardBody,
  CardFooter,
  CardHeader,
  FormControl,
  Heading,
  HStack,
  IconButton,
  List,
  ListItem,
  Menu,
  MenuButton,
  MenuItem,
  MenuList,
  Switch,
  useDisclosure,
  VStack,
} from '@chakra-ui/react'
import { ChevronDownIcon } from '@chakra-ui/icons'

import ConfirmationDialog from '@/components/Modal/ConfirmationDialog.tsx'
import type { FilterConfig, FilterConfigurationOption } from '@/modules/Workspace/components/filters/types.ts'
import { KEY_FILTER_CURRENT, KEY_FILTER_CONFIGURATIONS } from '@/modules/Workspace/components/filters/types.ts'
import { ConfigurationSave } from '@/modules/Workspace/components/filters/index.ts'

interface QuickFilterProps {
  onChange?: (values: SingleValue<FilterConfigurationOption>) => void
  onNewQuickFilter?: (name: string) => void
  isFilterActive: boolean
}

const QuickFilters: FC<QuickFilterProps> = ({ onNewQuickFilter, onChange, isFilterActive }) => {
  const { t } = useTranslation()
  const [configurations, setConfigurations] = useLocalStorage<FilterConfigurationOption[]>(
    KEY_FILTER_CONFIGURATIONS,
    []
  )
  const [currentState] = useLocalStorage<FilterConfig>(KEY_FILTER_CURRENT, {
    options: { isLiveUpdate: false, joinOperator: 'OR' },
  })

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

  const handleConfigSave = (name: string) => {
    const newConfig: FilterConfigurationOption = {
      label: name,
      filter: currentState,
      isActive: false,
    }
    setConfigurations((old) => [...old, newConfig])
    onNewQuickFilter?.(name)
  }

  const handleQuickFilterActivate = (config: FilterConfigurationOption, activate: boolean) => {
    const newState: FilterConfigurationOption = { ...config, isActive: activate }
    setConfigurations((old) => {
      const state = old.findIndex((e) => e.label === config.label)
      if (state !== -1) {
        old[state] = newState
      }
      return old
    })
    onChange?.(newState)
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
            <Heading as="h2" size="sm">
              {t('workspace.searchToolbox.quickFilters.label')}
            </Heading>
          </HStack>
        </CardHeader>
        <CardBody as={VStack} gap={3}>
          <List w="-webkit-fill-available">
            {configurations.map((config) => (
              <ListItem key={config.label} my={1}>
                <FormControl as={HStack} id="workspace-filter-quick">
                  <Switch
                    flex={1}
                    data-testid="workspace-filter-quick-label"
                    onChange={(e) => handleQuickFilterActivate(config, e.target.checked)}
                    isChecked={config.isActive}
                  >
                    {config.label}
                  </Switch>
                  <Menu id="filter-quick">
                    <MenuButton
                      variant="outline"
                      size="xs"
                      as={IconButton}
                      icon={<ChevronDownIcon />}
                      aria-label={t('workspace.searchToolbox.quickFilters.action.aria-label')}
                    />
                    <MenuList>
                      <MenuItem data-testid="workspace-filter-quick-view">
                        {t('workspace.searchToolbox.quickFilters.action.edit')}
                      </MenuItem>
                      <MenuItem
                        data-testid="workspace-filter-quick-delete"
                        onClick={() => handleOpenConfirmation(config.label)}
                      >
                        {t('workspace.searchToolbox.quickFilters.action.delete')}
                      </MenuItem>
                    </MenuList>
                  </Menu>
                </FormControl>
              </ListItem>
            ))}
          </List>
        </CardBody>
        <CardFooter>
          <ConfigurationSave
            isFilterActive={isFilterActive}
            configurations={configurations}
            onSave={handleConfigSave}
          />
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
