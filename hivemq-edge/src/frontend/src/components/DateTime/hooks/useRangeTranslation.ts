import { useTranslation } from 'react-i18next'

import { RangeOption } from '../types.ts'

export const useRangeTranslation = () => {
  const i18next = useTranslation('components')

  const translateBadgeFrom = (range: RangeOption) => {
    if (!range.duration) return undefined

    const entries = Object.entries(range.duration.toObject())
    if (!entries.length) return undefined

    const [key, value] = entries[0]
    return i18next.t(`DateTimeRangeSelector.label.badge.${key}`, { value })
  }

  return { ...i18next, translateBadgeFrom }
}
