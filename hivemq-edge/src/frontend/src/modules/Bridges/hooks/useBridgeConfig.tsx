import type { Bridge } from '@/api/__generated__'
import type { Dispatch, FunctionComponent, PropsWithChildren, SetStateAction } from 'react'
import { createContext, useContext, useState } from 'react'
import { bridgeInitialState } from '@/modules/Bridges/utils/defaults.utils.ts'

interface BridgeContextProps {
  bridge: Bridge
  setBridge: Dispatch<SetStateAction<Bridge>>
}

export const BridgeContext = createContext<BridgeContextProps | null>(null)

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
