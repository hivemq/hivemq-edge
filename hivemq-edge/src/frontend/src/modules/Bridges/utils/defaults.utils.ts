import type { Bridge } from '@/api/__generated__'

// TODO[NVL] The number and booleans should all be coming from the openAPI specs since they are all mandatory (see $Bridge.properties.cleanStart.isRequired)
export const bridgeInitialState: Bridge = {
  cleanStart: true,
  host: '',
  keepAlive: 60,
  id: '',
  port: 1883,
  sessionExpiry: 3600,
  persist: true,
  loopPreventionHopCount: 1,
}
