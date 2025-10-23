import type { FC } from 'react'
import { useTranslation } from 'react-i18next'
import { FormControl, FormLabel, Text } from '@chakra-ui/react'
import { chakraComponents, type MultiValue, type MultiValueProps, Select } from 'chakra-react-select'

import { Status } from '@/api/__generated__'
import type { FilterCriteriaProps, FilterStatusOption } from '@/modules/Workspace/components/filters/types.ts'
import { filterContainerStyle } from '@/modules/Workspace/components/filters/filters.utils.ts'

type FilterStatusProps = FilterCriteriaProps<MultiValue<FilterStatusOption>>

const FilterStatus: FC<FilterStatusProps> = ({ onChange, value, isDisabled }) => {
  const { t } = useTranslation()

  const options: FilterStatusOption[] = [
    {
      label: t('hivemq.connection.status', { context: Status.connection.CONNECTED }),
      status: Status.connection.CONNECTED,
    },
    {
      label: t('hivemq.connection.status', { context: Status.connection.STATELESS }),
      status: Status.connection.STATELESS,
    },
    {
      label: t('hivemq.connection.status', { context: Status.connection.DISCONNECTED }),
      status: Status.connection.DISCONNECTED,
    },
    {
      label: t('hivemq.connection.status', { context: Status.runtime.STOPPED }),
      status: Status.runtime.STOPPED,
    },
    {
      label: t('hivemq.connection.status', { context: Status.connection.ERROR }),
      status: Status.connection.ERROR,
    },
  ]

  const handleChange = (values: MultiValue<FilterStatusOption>) => {
    onChange?.(values)
  }

  return (
    <FormControl variant="horizontal" id="workspace-filter-status" isDisabled={isDisabled}>
      <FormLabel fontSize="sm" htmlFor="workspace-filter-status-input">
        {t('workspace.searchToolbox.byStatus.label')}
      </FormLabel>
      <Select<FilterStatusOption, true>
        isClearable
        isMulti
        id="workspace-filter-status-trigger"
        inputId="workspace-filter-status-input"
        instanceId="status"
        options={options}
        value={value}
        getOptionValue={(option) => option.status}
        onChange={handleChange}
        placeholder={t('workspace.searchToolbox.byStatus.placeholder')}
        noOptionsMessage={() => t('workspace.searchToolbox.byStatus.noOptions')}
        size="sm"
        chakraStyles={{
          container: filterContainerStyle,
        }}
        components={{
          MultiValue: (props: MultiValueProps<FilterStatusOption, true>) => (
            <chakraComponents.MultiValue {...props}>
              <Text data-testid="workspace-filter-status-values">{props.data.label}</Text>
            </chakraComponents.MultiValue>
          ),
        }}
      />
    </FormControl>
  )
}

export default FilterStatus
