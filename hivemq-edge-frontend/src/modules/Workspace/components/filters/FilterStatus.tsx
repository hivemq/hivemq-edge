import type { FC } from 'react'
import { useTranslation } from 'react-i18next'
import { FormControl, FormLabel } from '@chakra-ui/react'
import { type MultiValue, Select } from 'chakra-react-select'

import { Status } from '@/api/__generated__'
import type { FilterStatusOption } from '@/modules/Workspace/components/filters/types.ts'
import { filterContainerStyle } from '@/modules/Workspace/components/filters/filters.utils.ts'

interface FilterStatusProps {
  onChange?: (values: MultiValue<FilterStatusOption>) => void
}

const FilterStatus: FC<FilterStatusProps> = ({ onChange }) => {
  const { t } = useTranslation()

  const options: FilterStatusOption[] = [
    {
      label: t('hivemq.connection.status', { context: Status.connection.CONNECTED }),
      status: Status.connection.CONNECTED,
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
    <FormControl variant="horizontal">
      <FormLabel fontSize="sm" htmlFor="workspace-filter-status">
        {t('workspace.searchToolbox.byStatus.label')}
      </FormLabel>
      <Select<FilterStatusOption, true>
        isClearable
        isMulti
        inputId="workspace-filter-status"
        options={options}
        getOptionValue={(option) => option.status}
        onChange={handleChange}
        placeholder={t('workspace.searchToolbox.byStatus.placeholder')}
        noOptionsMessage={() => t('workspace.searchToolbox.byStatus.noOptions')}
        menuPortalTarget={document.body}
        size="sm"
        chakraStyles={{
          container: filterContainerStyle,
        }}
      />
    </FormControl>
  )
}

export default FilterStatus
