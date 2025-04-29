import { DateTime } from 'luxon'

export const downloadTimeStamp = () =>
  DateTime.now().startOf('second').toISO({ suppressMilliseconds: true, suppressSeconds: true, includeOffset: false })

export function downloadJSON<T>(name: string, source: T) {
  const jsonString = `data:text/json;charset=utf-8,${encodeURIComponent(JSON.stringify(source))}`
  const link = document.createElement('a')
  link.href = jsonString
  link.download = `${name}-${downloadTimeStamp()}.json`

  link.click()
}
