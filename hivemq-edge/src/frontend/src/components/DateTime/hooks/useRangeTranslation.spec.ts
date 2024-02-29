import { renderHook } from '@testing-library/react'
import { describe, expect } from 'vitest'
import { Duration } from 'luxon'

import '@/config/i18n.config.ts'

import { RangeOption } from '../types.ts'
import { MOCK_RANGE_OPTION } from '../utils/range-option.mocks.ts'
import { useRangeTranslation } from './useRangeTranslation.ts'

interface TestSuite {
  range: RangeOption
  badge: string | undefined
}

const allTests: TestSuite[] = [
  { range: { ...MOCK_RANGE_OPTION, duration: undefined }, badge: undefined },
  { range: { ...MOCK_RANGE_OPTION, duration: Duration.fromObject({ second: 1 }) }, badge: '1s' },
  { range: { ...MOCK_RANGE_OPTION, duration: Duration.fromObject({ second: 10 }) }, badge: '10s' },
  { range: { ...MOCK_RANGE_OPTION, duration: Duration.fromObject({ minute: 1 }) }, badge: '1m' },
  { range: { ...MOCK_RANGE_OPTION, duration: Duration.fromObject({ minute: 10 }) }, badge: '10m' },
  { range: { ...MOCK_RANGE_OPTION, duration: Duration.fromObject({ hour: 1 }) }, badge: '1h' },
  { range: { ...MOCK_RANGE_OPTION, duration: Duration.fromObject({ hour: 10 }) }, badge: '10h' },
  { range: { ...MOCK_RANGE_OPTION, duration: Duration.fromObject({ day: 1 }) }, badge: '1d' },
  { range: { ...MOCK_RANGE_OPTION, duration: Duration.fromObject({ day: 10 }) }, badge: '10d' },
  { range: { ...MOCK_RANGE_OPTION, duration: Duration.fromObject({ week: 1 }) }, badge: '1w' },
  { range: { ...MOCK_RANGE_OPTION, duration: Duration.fromObject({ week: 10 }) }, badge: '10w' },
  { range: { ...MOCK_RANGE_OPTION, duration: Duration.fromObject({ months: 1 }) }, badge: '1mo' },
  { range: { ...MOCK_RANGE_OPTION, duration: Duration.fromObject({ months: 10 }) }, badge: '10mo' },
  { range: MOCK_RANGE_OPTION, badge: '1h' },
]

describe('useRangeTranslation()', () => {
  it.each<TestSuite>(allTests)('should return $badge for $range.duration.values', ({ range, badge }) => {
    const { result } = renderHook(useRangeTranslation)

    expect(result.current.translateBadgeFrom(range)).toEqual(badge)
  })
})
