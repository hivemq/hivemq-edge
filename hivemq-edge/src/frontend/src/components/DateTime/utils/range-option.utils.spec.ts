import { describe, expect } from 'vitest'
import { DateTime, Duration } from 'luxon'

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
]

describe('makeDefaultRangeOption', () => {
  it.each<DefaultOptionTestSuite>(allTests)('should return $nbItems for $min $max', ({ min, max, items }) => {
    const tt = makeDefaultRangeOption(min, max)
    expect(tt.map((e) => e.value)).toEqual(items)
  })
})
