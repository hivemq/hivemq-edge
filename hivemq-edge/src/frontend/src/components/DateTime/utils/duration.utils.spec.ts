import { describe, expect } from 'vitest'
import { DateTime } from 'luxon'

import { toHuman } from './duration.utils.ts'
import { MOCK_DATE_TIME_NOW } from './range-option.mocks.ts'

interface ToHumanTestSuite {
  date: DateTime
  expected: string
}

const allTests: ToHumanTestSuite[] = [
  { date: MOCK_DATE_TIME_NOW.minus({ month: 2 }), expected: '2 months ago' },
  { date: MOCK_DATE_TIME_NOW.minus({ days: 9 }), expected: '9 days ago' },
  { date: MOCK_DATE_TIME_NOW.minus({ second: 1 }), expected: '1 second ago' },
  { date: MOCK_DATE_TIME_NOW.plus({ days: 1 }), expected: 'in 1 day' },
  { date: MOCK_DATE_TIME_NOW.plus({ days: 3 }), expected: 'in 3 days' },
  { date: MOCK_DATE_TIME_NOW.plus({ days: 3, hour: 19 }), expected: 'in 3 days' },
]

describe('toHuman', () => {
  it.each<ToHumanTestSuite>(allTests)('should return $expected', ({ date, expected }) => {
    expect(toHuman(date, MOCK_DATE_TIME_NOW)).toEqual(expected)
  })
})
