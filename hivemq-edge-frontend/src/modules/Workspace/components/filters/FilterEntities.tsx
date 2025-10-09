import type { FC } from 'react'
import { FormControl, FormLabel } from '@chakra-ui/react'
import { Select } from 'chakra-react-select'
import type { MultiValue } from 'chakra-react-select'
import { useTranslation } from 'react-i18next'

import { filterContainerStyle } from '@/modules/Workspace/components/filters/filters.utils.ts'
import type { FilterEntitiesOption } from '@/modules/Workspace/components/filters/types.ts'
import { NodeTypes } from '@/modules/Workspace/types.ts'

interface FilterEntitiesProps {
  id?: string
  onChange?: (values: MultiValue<FilterEntitiesOption>) => void
}

const FilterEntities: FC<FilterEntitiesProps> = ({ onChange }) => {
  const { t } = useTranslation()

  const options: FilterEntitiesOption[] = [
    { label: t('workspace.searchToolbox.byEntity.categories.ADAPTER_NODE'), value: NodeTypes.ADAPTER_NODE },
    { label: t('workspace.searchToolbox.byEntity.categories.DEVICE_NODE'), value: NodeTypes.DEVICE_NODE },
    { label: t('workspace.searchToolbox.byEntity.categories.BRIDGE_NODE'), value: NodeTypes.BRIDGE_NODE },
    { label: t('workspace.searchToolbox.byEntity.categories.HOST_NODE'), value: NodeTypes.HOST_NODE },
    { label: t('workspace.searchToolbox.byEntity.categories.PULSE_NODE'), value: NodeTypes.PULSE_NODE },
    { label: t('workspace.searchToolbox.byEntity.categories.EDGE_NODE'), value: NodeTypes.EDGE_NODE },
    { label: t('workspace.searchToolbox.byEntity.categories.COMBINER_NODE'), value: NodeTypes.COMBINER_NODE },
    { label: t('workspace.searchToolbox.byEntity.categories.ASSETS_NODE'), value: NodeTypes.ASSETS_NODE },
  ]

  const handleChange = (values: MultiValue<FilterEntitiesOption>) => {
    onChange?.(values)
  }

  return (
    <FormControl variant="horizontal">
      <FormLabel fontSize="sm" htmlFor="workspace-filter-entities">
        {t('workspace.searchToolbox.byEntity.label')}
      </FormLabel>
      <Select<FilterEntitiesOption, true>
        isClearable
        isMulti
        inputId="workspace-filter-entities"
        options={options}
        getOptionValue={(option) => option.value}
        onChange={handleChange}
        placeholder={t('workspace.searchToolbox.byEntity.placeholder')}
        noOptionsMessage={() => t('workspace.searchToolbox.byEntity.noOptions')}
        menuPortalTarget={document.body}
        size="sm"
        chakraStyles={{
          container: filterContainerStyle,
        }}
      />
    </FormControl>
  )
}

export default FilterEntities
