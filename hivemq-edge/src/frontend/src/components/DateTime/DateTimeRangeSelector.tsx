import { FC, useState } from 'react'
import { DateTime } from 'luxon'
import { CreatableSelect, GroupBase, Options, OptionsOrGroups } from 'chakra-react-select'
import { useTranslation } from 'react-i18next'

import { Accessors, RangeOption } from './types.ts'
import Option from './components/Option.tsx'

const defaultRangeOption: readonly RangeOption[] = [
  { value: 'purple', label: 'last minute', color: '#5243AA' },
  { value: 'orange', label: 'last 5 minutes', color: '#FF8B00' },
  { value: 'yellow', label: 'last 15 minutes ', color: '#FFC400' },
  { value: 'green', label: 'last 30 minutes', color: '#36B37E' },
  { value: 'forest', label: 'last hour', color: '#00875A' },
  { value: 'slate', label: 'last 2 hours', color: '#253858' },
  { value: 'silver', label: 'last 5 hours', color: '#666666' },
  { value: 'more', label: 'More...', color: '#0052CC', isDisabled: true },
]

interface DateTimeRangeSelectorProps {
  min?: DateTime
  max?: DateTime
  value?: DateTime
}

const DateTimeRangeSelector: FC<DateTimeRangeSelectorProps> = () => {
  const { t } = useTranslation('components')
  const [options, setOptions] = useState(defaultRangeOption)

  const compareOption = (inputValue = '', option: RangeOption, accessors: Accessors<RangeOption>) => {
    const candidate = String(inputValue).toLowerCase()
    const optionValue = String(accessors.getOptionValue(option)).toLowerCase()
    const optionLabel = String(accessors.getOptionLabel(option)).toLowerCase()
    return optionValue === candidate || optionLabel === candidate
  }

  const handleCreate = (inputValue: string) => {
    const newOption = { value: inputValue, label: inputValue, color: '#0052CC', isDisabled: true }
    setOptions((prev) => [...prev, newOption])
  }

  const isValidNewOption = (
    inputValue: string,
    selectValue: Options<RangeOption>,
    selectOptions: OptionsOrGroups<RangeOption, GroupBase<RangeOption>>,
    accessors: Accessors<RangeOption>
  ) => {
    return !(
      !inputValue ||
      selectValue.some((option) => compareOption(inputValue, option, accessors)) ||
      selectOptions.some((option) => compareOption(inputValue, option as RangeOption, accessors))
    )
  }

  return (
    <CreatableSelect<RangeOption>
      size={'sm'}
      menuPortalTarget={document.body}
      // value={{ value: columnFilterValue, label: columnFilterValue }}
      // onChange={(item) => setFilterValue(item?.value)}
      options={options}
      noOptionsMessage={() => t('DateTimeRangeSelector.noOptionsMessage')}
      placeholder={t('DateTimeRangeSelector.placeholder')}
      formatCreateLabel={(e) => t('DateTimeRangeSelector.formatCreateLabel', { date: e })}
      aria-label={t('DateTimeRangeSelector.ariaLabel') as string}
      isClearable={true}
      isMulti={false}
      components={{
        DropdownIndicator: null,
        Option,
      }}
      onCreateOption={handleCreate}
      isValidNewOption={isValidNewOption}
    />
  )
}

export default DateTimeRangeSelector
