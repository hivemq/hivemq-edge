import { DateTime } from 'luxon'

export const downloadTimeStamp = () =>
  DateTime.now().startOf('second').toISO({ suppressMilliseconds: true, suppressSeconds: true, includeOffset: false })
