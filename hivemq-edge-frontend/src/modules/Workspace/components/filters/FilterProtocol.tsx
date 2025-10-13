import type { FC } from 'react'
import { useMemo } from 'react'
import { FormControl, FormLabel, Text } from '@chakra-ui/react'
import { chakraComponents, type MultiValueProps, Select } from 'chakra-react-select'
import type { MultiValue } from 'chakra-react-select'
import { useTranslation } from 'react-i18next'

import { useGetAdapterTypes } from '@/api/hooks/useProtocolAdapters/useGetAdapterTypes.ts'
import { useListProtocolAdapters } from '@/api/hooks/useProtocolAdapters/useListProtocolAdapters.ts'
import { filterContainerStyle } from '@/modules/Workspace/components/filters/filters.utils.ts'
import type { FilterAdapterOption } from '@/modules/Workspace/components/filters/types.ts'

interface FilterProtocolProps {
  onChange?: (values: MultiValue<FilterAdapterOption>) => void
  value?: MultiValue<FilterAdapterOption>
}

const FilterProtocol: FC<FilterProtocolProps> = ({ onChange, value }) => {
  const { t } = useTranslation()
  const { data } = useGetAdapterTypes()
  const { data: adapters } = useListProtocolAdapters()

  const existingAdapter = useMemo(() => {
    if (!adapters) return []
    return adapters.map((e) => e.type as string)
  }, [adapters])

  const options = useMemo<FilterAdapterOption[]>(() => {
    if (!data || !data.items) return []
    return data.items.map((protocol) => ({
      type: protocol.id as string,
      label: protocol.protocol as string,
    }))
  }, [data])

  const handleChange = (values: MultiValue<FilterAdapterOption>) => {
    onChange?.(values)
  }

  return (
    <FormControl variant="horizontal" id="workspace-filter-protocol">
      <FormLabel fontSize="sm" htmlFor="workspace-filter-protocol-input">
        {t('workspace.searchToolbox.byProtocol.label')}
      </FormLabel>
      <Select<FilterAdapterOption, true>
        isClearable
        isMulti
        id="workspace-filter-protocol-trigger"
        inputId="workspace-filter-protocol-input"
        instanceId="protocol"
        options={options}
        value={value}
        getOptionValue={(option) => option.type}
        onChange={handleChange}
        placeholder={t('workspace.searchToolbox.byProtocol.placeholder')}
        noOptionsMessage={() => t('workspace.searchToolbox.byProtocol.noOptions')}
        size="sm"
        chakraStyles={{
          container: filterContainerStyle,
        }}
        filterOption={(option, value) =>
          existingAdapter.includes(option.data.type) &&
          (new RegExp(value, 'i').test(option.value) || new RegExp(value, 'i').test(option.label))
        }
        components={{
          MultiValue: (props: MultiValueProps<FilterAdapterOption, true>) => (
            <chakraComponents.MultiValue {...props}>
              <Text data-testid="workspace-filter-protocol-values">{props.data.label}</Text>
            </chakraComponents.MultiValue>
          ),
        }}
      />
    </FormControl>
  )
}

export default FilterProtocol
