import { FC, useEffect, useState } from 'react'
import { DateTime } from 'luxon'
import { ActionMeta, CreatableSelect, SingleValue } from 'chakra-react-select'
import { useTranslation } from 'react-i18next'

import { RangeOption } from './types.ts'
import Option from './components/Option.tsx'
import { makeDefaultRangeOption } from './utils/range-option.utils.ts'

interface DateTimeRangeSelectorProps {
  min?: DateTime
  max?: DateTime
  value?: DateTime
  setFilterValue?: (value: number[] | undefined) => void
}

const DateTimeRangeSelector: FC<DateTimeRangeSelectorProps> = ({ min, max, setFilterValue }) => {
  const { t } = useTranslation('components')
  const [options, setOptions] = useState<RangeOption[]>([])

  useEffect(() => {
    const dd = makeDefaultRangeOption(min, max)
    if (dd) setOptions(dd)
  }, [min, max])

  const handleCreate = (inputValue: string) => {
    const newOption: RangeOption = { value: inputValue, label: inputValue, colorScheme: '#0052CC' }
    setOptions((prev) => {
      const old = [...prev]
      const last = old.pop()
      const newOptions = [...old, newOption]
      if (last) newOptions.push(last)
      return newOptions
    })
  }

  const onHandleChange = (newValue: SingleValue<RangeOption>, actionMeta: ActionMeta<RangeOption>) => {
    if (newValue?.duration?.isValid) {
      const now = DateTime.now()
      const min = now.minus(newValue.duration)
      setFilterValue?.([min.toMillis(), now.toMillis()])
    } else if (actionMeta.action === 'clear') {
      setFilterValue?.(undefined)
    }
  }

  return (
    <CreatableSelect<RangeOption>
      chakraStyles={{ menuList: (provided) => ({ ...provided, width: '200px' }) }}
      size={'sm'}
      menuPortalTarget={document.body}
      // value={{ value: columnFilterValue, label: columnFilterValue }}
      onChange={onHandleChange}
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
      // TODO[NVL} Do not allow manual editing of custom date
      isValidNewOption={() => false}
    />
  )
}

export default DateTimeRangeSelector
