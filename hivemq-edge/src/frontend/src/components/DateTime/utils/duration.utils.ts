import { DateTime } from 'luxon'
import { DurationUnits } from 'luxon/src/duration'

// Better Duration.toHuman support https://github.com/moment/luxon/issues/1134
export const toHuman = (timestamp: DateTime, alternativeNow?: DateTime) => {
  const units: DurationUnits = ['weeks', 'days', 'hours', 'minutes', 'seconds']

  // mostly used for testing
  const diff = alternativeNow ? timestamp.diff(alternativeNow, units) : timestamp.diffNow(units)

  const rescaledDuration = diff
    .negate()
    .mapUnits((x) => Math.floor(x))
    .rescale()

  return DateTime.local().minus(rescaledDuration).toRelative()
}
