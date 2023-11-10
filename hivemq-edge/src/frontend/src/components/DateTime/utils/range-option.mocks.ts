import { RangeOption } from '@/components/DateTime/types.ts'
import { DateTime, Duration } from 'luxon'

export const MOCK_RANGE_OPTION: RangeOption = {
  value: 'range1',
  label: 'Range 1',
  colorScheme: 'yellow',
  duration: Duration.fromObject({ hour: 1 }),
}

export const MOCK_DATE_TIME_NOW = DateTime.fromObject({ year: 2023, month: 11, day: 10 })
