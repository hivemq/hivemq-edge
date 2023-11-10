import { DateTime, Duration } from 'luxon'
import { RangeOption } from '@/components/DateTime/types.ts'

const defaultRangeOption: readonly RangeOption[] = [
  { value: 'more', label: 'More...', colorScheme: 'yellow', isCommand: true, isDisabled: true },
]

export const sortRangeOption = (a: RangeOption, b: RangeOption) => {
  // not sure that's the case
  if (!a.duration || !b.duration) return 0

  if (a.duration?.toMillis() < b.duration?.toMillis()) return -1
  else if (a.duration?.toMillis() > b.duration?.toMillis()) return 1
  return 0
}

export const makeDefaultRangeOption = (min: DateTime | undefined, max: DateTime | undefined): RangeOption[] => {
  if (!min || !max) return []

  // TODO[NVL} wrong!!!
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
          duration: Duration.fromDurationLike({ hour: 2 }),
        },
        {
          value: 'hour6',
          label: 'last 6 hours',
          colorScheme: 'blue',
          duration: Duration.fromDurationLike({ hour: 6 }),
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

  const sortedOptions = [...options].sort(sortRangeOption)
  sortedOptions.push(...defaultRangeOption)

  return sortedOptions
}
