import { describe, expect } from 'vitest'
import { DateTime } from 'luxon'

import { toHuman } from './duration.utils.ts'
import { MOCK_DATE_TIME_NOW } from './range-option.mocks.ts'

interface ToHumanTestSuite {
  date: DateTime
  expected: string | null
}

const allTests: ToHumanTestSuite[] = [
  { date: MOCK_DATE_TIME_NOW.minus({ month: 2 }), expected: '2 months ago' },
  { date: MOCK_DATE_TIME_NOW.minus({ days: 9 }), expected: '9 days ago' },
  { date: MOCK_DATE_TIME_NOW.minus({ minutes: 3 }), expected: '3 minutes ago' },
  { date: MOCK_DATE_TIME_NOW.minus({ minutes: 1 }), expected: '1 minute ago' },
  { date: MOCK_DATE_TIME_NOW.minus({ seconds: 50 }), expected: '1 minute ago' },
  { date: MOCK_DATE_TIME_NOW.minus({ seconds: 30 }), expected: '1 minute ago' },
  { date: MOCK_DATE_TIME_NOW.minus({ seconds: 29 }), expected: null },
  { date: MOCK_DATE_TIME_NOW.minus({ second: 1 }), expected: null },
]

describe('toHuman', () => {
  it.each<ToHumanTestSuite>(allTests)('should return $expected', ({ date, expected }) => {
    expect(toHuman(date, MOCK_DATE_TIME_NOW)).toEqual(expected)
  })
})
