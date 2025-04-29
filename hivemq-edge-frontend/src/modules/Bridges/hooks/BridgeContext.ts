import type { Dispatch, SetStateAction } from 'react'
import { createContext } from 'react'
import type { Bridge } from '@/api/__generated__'

export interface BridgeContextProps {
  bridge: Bridge
  setBridge: Dispatch<SetStateAction<Bridge>>
}

/* istanbul ignore next -- @preserve */
export const BridgeContext = createContext<BridgeContextProps | null>(null)
