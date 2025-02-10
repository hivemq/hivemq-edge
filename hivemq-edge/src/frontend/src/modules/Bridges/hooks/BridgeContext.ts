import type { Dispatch, SetStateAction } from 'react'
import { createContext } from 'react'
import type { Bridge } from '@/api/__generated__'

interface BridgeContextProps {
  bridge: Bridge
  setBridge: Dispatch<SetStateAction<Bridge>>
}

export const BridgeContext = createContext<BridgeContextProps | null>(null)
