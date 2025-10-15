import type { FC } from 'react'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { FormControl, FormErrorMessage, FormHelperText, FormLabel, Input, InputGroup, VStack } from '@chakra-ui/react'
import { FaSave } from 'react-icons/fa'

import IconButton from '@/components/Chakra/IconButton.tsx'
import type { FilterConfigurationOption } from '@/modules/Workspace/components/filters/types.ts'

interface ConfigurationSaveProps {
  isFilterActive: boolean
  onSave?: (name: string) => void
  configurations: FilterConfigurationOption[]
}

const ConfigurationSave: FC<ConfigurationSaveProps> = ({ isFilterActive, onSave, configurations }) => {
  const { t } = useTranslation()
  const [filterName, setFilterName] = useState<string>('')

  // TODO[NVL] Should we check for duplicate in filter criteria?
  const isDuplicateError = configurations.some((e) => e.label === filterName)

  const handleSave = () => {
    onSave?.(filterName)
    setFilterName('')
  }

  return (
    <FormControl
      variant="horizontal"
      id="workspace-filter-configuration"
      isInvalid={isDuplicateError}
      isDisabled={!isFilterActive}
    >
      <FormLabel fontSize="sm" htmlFor="workspace-filter-configuration-input">
        {t('workspace.searchToolbox.configuration.label')}
      </FormLabel>
      <VStack alignItems="flex-start" gap={0}>
        <InputGroup size="sm" gap={2}>
          <Input
            id="workspace-filter-configuration-input"
            placeholder={t('workspace.searchToolbox.configuration.placeholder')}
            onChange={(event) => setFilterName(event.target.value)}
            value={filterName}
          />
          <IconButton
            icon={<FaSave />}
            id="workspace-filter-configuration-save"
            aria-label={t('workspace.searchToolbox.configuration.save')}
            isDisabled={!(filterName && !isDuplicateError)}
            onClick={handleSave}
          />
        </InputGroup>

        {!isDuplicateError && <FormHelperText>{t('workspace.searchToolbox.configuration.helper')}</FormHelperText>}
        {isDuplicateError && (
          <FormErrorMessage>{t('workspace.searchToolbox.configuration.error.noDuplicateName')}</FormErrorMessage>
        )}
      </VStack>
    </FormControl>
  )
}

export default ConfigurationSave
