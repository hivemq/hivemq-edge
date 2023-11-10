import { RangeOption } from '@/components/DateTime/types.ts'
import { Duration } from 'luxon'

export const MOCK_RANGE_OPTION: RangeOption = {
  value: 'range1',
  label: 'Range 1',
  colorScheme: 'yellow',
  duration: Duration.fromObject({ hour: 1 }),
}
