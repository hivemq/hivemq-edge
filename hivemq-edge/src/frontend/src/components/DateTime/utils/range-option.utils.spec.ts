import { describe, expect } from 'vitest'
import type { DateTime } from 'luxon'
import { Duration } from 'luxon'

import { makeDefaultRangeOption, sortRangeOption } from '../utils/range-option.utils.ts'
import { MOCK_DATE_TIME_NOW, MOCK_RANGE_OPTION } from '../utils/range-option.mocks.ts'

describe('sortRangeOption', () => {
  it('should sort RangeOption based on duration ', () => {
    expect(sortRangeOption(MOCK_RANGE_OPTION, MOCK_RANGE_OPTION)).toEqual(0)
    expect(sortRangeOption({ ...MOCK_RANGE_OPTION, duration: undefined }, MOCK_RANGE_OPTION)).toEqual(0)
    expect(sortRangeOption(MOCK_RANGE_OPTION, { ...MOCK_RANGE_OPTION, duration: undefined })).toEqual(0)
    expect(
      sortRangeOption(
        { ...MOCK_RANGE_OPTION, duration: Duration.fromObject({ minute: 1 }) },
        { ...MOCK_RANGE_OPTION, duration: Duration.fromObject({ minute: 1 }) }
      )
    ).toEqual(0)
    expect(
      sortRangeOption(
        { ...MOCK_RANGE_OPTION, duration: Duration.fromObject({ minute: 10 }) },
        { ...MOCK_RANGE_OPTION, duration: Duration.fromObject({ minute: 1 }) }
      )
    ).toEqual(1)
    expect(
      sortRangeOption(
        { ...MOCK_RANGE_OPTION, duration: Duration.fromObject({ minute: 1 }) },
        { ...MOCK_RANGE_OPTION, duration: Duration.fromObject({ minute: 10 }) }
      )
    ).toEqual(-1)
    expect(
      sortRangeOption(
        { ...MOCK_RANGE_OPTION, duration: Duration.fromObject({ minute: 1 }) },
        { ...MOCK_RANGE_OPTION, duration: Duration.fromObject({ hour: 10 }) }
      )
    ).toEqual(-1)
  })
})

interface DefaultOptionTestSuite {
  min: DateTime | undefined
  max: DateTime | undefined
  items: string[]
}

const allTests: DefaultOptionTestSuite[] = [
  { min: MOCK_DATE_TIME_NOW, max: undefined, items: [] },
  { min: undefined, max: MOCK_DATE_TIME_NOW, items: [] },
  {
    min: MOCK_DATE_TIME_NOW,
    max: MOCK_DATE_TIME_NOW.plus({ minute: 20 }),

    items: ['minute1', 'minute5', 'minute15', 'minute30', 'more'],
  },
  {
    min: MOCK_DATE_TIME_NOW,
    max: MOCK_DATE_TIME_NOW.plus({ month: 2 }),

    items: ['month1', 'more'],
  },
  {
    min: MOCK_DATE_TIME_NOW,
    max: MOCK_DATE_TIME_NOW.plus({ week: 2 }),

    items: ['week1', 'more'],
  },
  {
    min: MOCK_DATE_TIME_NOW,
    max: MOCK_DATE_TIME_NOW.plus({ day: 2 }),

    items: ['day1', 'more'],
  },
  {
    min: MOCK_DATE_TIME_NOW,
    max: MOCK_DATE_TIME_NOW.plus({ hour: 2 }),

    items: ['hour1', 'hour2', 'hour6', 'more'],
  },
]

describe('makeDefaultRangeOption', () => {
  it.each<DefaultOptionTestSuite>(allTests)('should return $nbItems for $min $max', ({ min, max, items }) => {
    const rangeOptions = makeDefaultRangeOption(min, max)
    expect(rangeOptions.map((range) => range.value)).toEqual(items)
  })
})
