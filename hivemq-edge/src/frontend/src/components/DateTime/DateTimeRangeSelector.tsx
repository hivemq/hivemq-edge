import { FC, useEffect, useState } from 'react'
import { DateTime, Duration } from 'luxon'
import { ActionMeta, CreatableSelect, SingleValue } from 'chakra-react-select'
import { useTranslation } from 'react-i18next'

import { RangeOption } from './types.ts'
import Option from './components/Option.tsx'

const defaultRangeOption: readonly RangeOption[] = [
  { value: 'more', label: 'More...', colorScheme: 'yellow', isCommand: true, isDisabled: true },
]

const makeDefaultRangeOption = (min: DateTime | undefined, max: DateTime | undefined): RangeOption[] => {
  if (!min || !max) return []

  const diff = max.diff(min, ['months', 'weeks', 'days', 'hours', 'minutes']).toObject()
  const options: RangeOption[] = []

  if (diff['months']) {
    options.push({
      value: 'month1',
      label: 'last month',
      colorScheme: 'whiteAlpha',
      duration: Duration.fromDurationLike({ month: 1 }),
    })
  }
  if (diff['weeks']) {
    options.push({
      value: 'week1',
      label: 'last week',
      colorScheme: 'red',
      duration: Duration.fromDurationLike({ week: 1 }),
    })
  }
  if (diff['days']) {
    options.push({
      value: 'day1',
      label: 'last day',
      colorScheme: 'green',
      duration: Duration.fromDurationLike({ day: 1 }),
    })
  }
  if (diff['hours']) {
    options.push(
      ...[
        {
          value: 'hour1',
          label: 'last hour',
          colorScheme: 'blue',
          duration: Duration.fromDurationLike({ hour: 1 }),
        },
        {
          value: 'hour2',
          label: 'last 2 hours',
          colorScheme: 'blue',
          duration: Duration.fromDurationLike({ hours: 2 }),
        },
        {
          value: 'hour6',
          label: 'last 6 hours',
          colorScheme: 'blue',
          duration: Duration.fromDurationLike({ hours: 6 }),
        },
      ]
    )
  }
  if (diff['minutes']) {
    options.push(
      ...[
        {
          value: 'minute1',
          label: 'last minute',
          colorScheme: 'orange',
          duration: Duration.fromDurationLike({ minute: 1 }),
        },
        {
          value: 'minute5',
          label: 'last 5 minutes',
          colorScheme: 'orange',
          duration: Duration.fromDurationLike({ minute: 5 }),
        },
        {
          value: 'minute15',
          label: 'last 15 minutes ',
          colorScheme: 'orange',
          duration: Duration.fromDurationLike({ minute: 15 }),
        },
        {
          value: 'minute30',
          label: 'last 30 minutes',
          colorScheme: 'orange',
          duration: Duration.fromDurationLike({ minute: 30 }),
        },
      ]
    )
  }

  options.push(...defaultRangeOption)

  return options
}

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
