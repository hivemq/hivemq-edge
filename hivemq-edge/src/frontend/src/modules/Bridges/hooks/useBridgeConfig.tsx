import { useContext } from 'react'
import type { Bridge } from '@/api/__generated__'
import { BridgeContext } from './BridgeContext'

// TODO The number and booleans should all be coming from the openAPI specs since they are all mandatory (see $Bridge.properties.cleanStart.isRequired)

export const bridgeInitialState: Bridge = {
  cleanStart: true,
  host: '',
  keepAlive: 60,
  id: '',
  port: 1883,
  sessionExpiry: 3600,
  persist: true,
}

export const useBridgeSetup = () => {
  const context = useContext(BridgeContext)
  if (context === null) {
    throw Error('useBridgeSetup must be used within a BridgeContext')
  }
  return context
}
