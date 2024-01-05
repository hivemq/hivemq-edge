import { DateTime } from 'luxon'
import { DurationUnits } from 'luxon/src/duration'

export const toHuman = (timestamp: DateTime, alternativeNow?: DateTime) => {
  const units: DurationUnits = ['weeks', 'days', 'hours', 'minutes', 'seconds']

  // mostly used for testing
  const diff = alternativeNow ? timestamp.diff(alternativeNow, units) : timestamp.diffNow(units)

  const rescaledDuration = diff
    .negate()
    .mapUnits((x) => Math.floor(x))
    .rescale()

  if (rescaledDuration.valueOf() < 30 * 1000) return null
  if (rescaledDuration.valueOf() < 60 * 1000)
    return DateTime.local()
      .minus(rescaledDuration.set({ minute: 1 }))
      .toRelative()

  return DateTime.local().minus(rescaledDuration).toRelative()
}
