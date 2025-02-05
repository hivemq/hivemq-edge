import type { FunctionComponent, PropsWithChildren } from 'react'
import { useState } from 'react'
import type { Bridge } from '@/api/__generated__'
import { BridgeContext } from '@/modules/Bridges/hooks/BridgeContext.ts'
import { bridgeInitialState } from '@/modules/Bridges/hooks/useBridgeConfig.tsx'

export const BridgeProvider: FunctionComponent<PropsWithChildren> = ({ children }) => {
  const [bridge, setBridge] = useState<Bridge>(bridgeInitialState)

  return <BridgeContext.Provider value={{ bridge, setBridge }}>{children}</BridgeContext.Provider>
}
