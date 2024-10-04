import type { DeviceDataPoint } from '@/api/__generated__'

export const formatTagDataPoint = (data?: DeviceDataPoint) => {
  if (data) return JSON.stringify(data, null, 4)
  return '< unknown format >'
}
