import type { FC } from 'react'
import { useMemo } from 'react'
import { FormControl, FormLabel, Text } from '@chakra-ui/react'
import { chakraComponents, type MultiValueProps, Select } from 'chakra-react-select'
import type { MultiValue } from 'chakra-react-select'
import { useTranslation } from 'react-i18next'

import { filterContainerStyle } from '@/modules/Workspace/components/filters/filters.utils.ts'
import type { FilterEntitiesOption } from '@/modules/Workspace/components/filters/types.ts'
import useWorkspaceStore from '@/modules/Workspace/hooks/useWorkspaceStore.ts'
import { NodeTypes } from '@/modules/Workspace/types.ts'

interface FilterEntitiesProps {
  onChange?: (values: MultiValue<FilterEntitiesOption>) => void
  value?: MultiValue<FilterEntitiesOption>
}

const FilterEntities: FC<FilterEntitiesProps> = ({ onChange, value }) => {
  const { t } = useTranslation()
  const { nodes } = useWorkspaceStore()

  const existingNodeTypes = useMemo(() => {
    return new Set(nodes.map((e) => e.type as NodeTypes))
  }, [nodes])

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
    <FormControl variant="horizontal" id="workspace-filter-entities">
      <FormLabel fontSize="sm" htmlFor="workspace-filter-entities-input">
        {t('workspace.searchToolbox.byEntity.label')}
      </FormLabel>
      <Select<FilterEntitiesOption, true>
        isClearable
        isMulti
        id="workspace-filter-entities-trigger"
        inputId="workspace-filter-entities-input"
        instanceId="entities"
        options={options}
        value={value}
        getOptionValue={(option) => option.value}
        onChange={handleChange}
        placeholder={t('workspace.searchToolbox.byEntity.placeholder')}
        noOptionsMessage={() => t('workspace.searchToolbox.byEntity.noOptions')}
        size="sm"
        chakraStyles={{
          container: filterContainerStyle,
        }}
        filterOption={(option, value) => {
          return (
            existingNodeTypes.has(option.value as NodeTypes) &&
            (new RegExp(value, 'i').test(option.value) || new RegExp(value, 'i').test(option.label))
          )
        }}
        components={{
          MultiValue: (props: MultiValueProps<FilterEntitiesOption, true>) => (
            <chakraComponents.MultiValue {...props}>
              <Text data-testid="workspace-filter-entities-values">{props.data.label}</Text>
            </chakraComponents.MultiValue>
          ),
        }}
      />
    </FormControl>
  )
}

export default FilterEntities
