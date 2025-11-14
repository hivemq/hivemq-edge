import type { ProtocolAdapter as ApiProtocolAdapter } from '@/api/__generated__'
import { createContext } from 'react'

export interface ProtocolAdaptersContextValue {
  protocolAdapters: ApiProtocolAdapter[] | undefined
}

export const ProtocolAdaptersContext = createContext<ProtocolAdaptersContextValue>({
  protocolAdapters: undefined,
})
