import type { FC } from 'react'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useLocalStorage } from '@uidotdev/usehooks'
import type { ActionMeta, SingleValue } from 'chakra-react-select'
import { CreatableSelect } from 'chakra-react-select'
import { FormControl, FormLabel } from '@chakra-ui/react'
import { MdDeleteOutline } from 'react-icons/md'

import IconButton from '@/components/Chakra/IconButton.tsx'
import { filterContainerStyle } from '@/modules/Workspace/components/filters/filters.utils.ts'
import type { FilterConfigurationOption } from '@/modules/Workspace/components/filters/types.ts'
import { KEY_FILTER_CONFIGURATIONS } from '@/modules/Workspace/components/filters/types.ts'

interface ConfigurationSelectorProps {
  id?: string
}

const ConfigurationSelector: FC<ConfigurationSelectorProps> = () => {
  const { t } = useTranslation()
  const [configurations, setConfigurations] = useLocalStorage<FilterConfigurationOption[]>(
    KEY_FILTER_CONFIGURATIONS,
    []
  )
  const [configuration, setConfiguration] = useState<FilterConfigurationOption | null>()

  const handleConfigDelete = () => {
    if (!configuration) return
    setConfigurations((old) => old.filter((option) => option.label !== configuration.label))
    setConfiguration(null)
  }
  const handleConfigChange = (
    option: SingleValue<FilterConfigurationOption>,
    actionMeta: ActionMeta<FilterConfigurationOption>
  ) => {
    if (actionMeta.action === 'create-option' && option) {
      const newConfig = {
        label: option.label,
        config: option.config,
        date: new Date().toISOString(),
      }
      setConfigurations((old) => [...old, newConfig])
    }
    setConfiguration(option)
  }

  return (
    <FormControl variant="horizontal">
      <FormLabel fontSize="sm" htmlFor="workspace-filter-save">
        {t('workspace.searchToolbox.configuration.label')}
      </FormLabel>
      <CreatableSelect<FilterConfigurationOption, false>
        options={configurations}
        inputId="workspace-filter-save"
        getOptionValue={(option) => option.label}
        value={configuration}
        isClearable
        onChange={handleConfigChange}
        placeholder={t('workspace.searchToolbox.configuration.placeholder')}
        size="sm"
        chakraStyles={{
          container: filterContainerStyle,
        }}
        formatCreateLabel={(inputValue) => t('workspace.searchToolbox.configuration.createLabel', { name: inputValue })}
      />
      <IconButton
        size="sm"
        aria-label={t('workspace.searchToolbox.configuration.delete')}
        icon={<MdDeleteOutline />}
        isDisabled={!configuration}
        onClick={handleConfigDelete}
      />
    </FormControl>
  )
}

export default ConfigurationSelector
