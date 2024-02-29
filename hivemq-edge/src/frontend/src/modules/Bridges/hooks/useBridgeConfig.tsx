import { Bridge } from '@/api/__generated__'
import {
  createContext,
  Dispatch,
  FunctionComponent,
  PropsWithChildren,
  SetStateAction,
  useContext,
  useState,
} from 'react'

interface BridgeContextProps {
  bridge: Bridge
  setBridge: Dispatch<SetStateAction<Bridge>>
}

export const BridgeContext = createContext<BridgeContextProps | null>(null)

// TODO The number and booleans should all be coming from the openAPI specs since they are all mandatory (see $Bridge.properties.cleanStart.isRequired)
// eslint-disable-next-line react-refresh/only-export-components
export const bridgeInitialState: Bridge = {
  cleanStart: true,
  host: '',
  keepAlive: 60,
  id: '',
  port: 1883,
  sessionExpiry: 3600,
  persist: true,
}

export const BridgeProvider: FunctionComponent<PropsWithChildren> = ({ children }) => {
  const [bridge, setBridge] = useState<Bridge>(bridgeInitialState)

  return <BridgeContext.Provider value={{ bridge, setBridge }}>{children}</BridgeContext.Provider>
}

// eslint-disable-next-line react-refresh/only-export-components
export const useBridgeSetup = () => {
  const context = useContext(BridgeContext)
  if (context === null) {
    throw Error('useBridgeSetup must be used within a BridgeContext')
  }
  return context
}
