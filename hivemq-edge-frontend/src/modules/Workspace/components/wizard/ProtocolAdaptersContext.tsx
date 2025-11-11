/**
 * Protocol Adapters Context
 *
 * Provides protocol adapter type information to wizard components
 * for capability checking without causing re-render loops.
 */

import { createContext, useContext, useMemo, type FC, type ReactNode } from 'react'
import { useGetAdapterTypes } from '@/api/hooks/useProtocolAdapters/useGetAdapterTypes'

interface ProtocolAdapter {
  id: string | undefined
  capabilities?: string[]
}

interface ProtocolAdaptersContextValue {
  protocolAdapters: ProtocolAdapter[] | undefined
}

export const ProtocolAdaptersContext = createContext<ProtocolAdaptersContextValue>({
  protocolAdapters: undefined,
})

export const useProtocolAdaptersContext = () => {
  return useContext(ProtocolAdaptersContext)
}

/**
 * Provider component that fetches protocol adapters and provides them via context
 */
export const ProtocolAdaptersProvider: FC<{ children: ReactNode }> = ({ children }) => {
  const { data: protocolAdapterTypes } = useGetAdapterTypes()

  const protocolAdaptersList = useMemo(() => {
    const list = protocolAdapterTypes?.items
      ?.filter((pa) => pa.id)
      ?.map((protocolAdapter) => ({
        id: protocolAdapter.id as string,
        capabilities: protocolAdapter.capabilities || [],
      }))

    console.log('📋 Protocol Adapters Provider:', {
      hasData: !!protocolAdapterTypes,
      itemsCount: protocolAdapterTypes?.items?.length,
      filteredCount: list?.length,
      list,
    })

    return list
  }, [protocolAdapterTypes])

  return (
    <ProtocolAdaptersContext.Provider value={{ protocolAdapters: protocolAdaptersList }}>
      {children}
    </ProtocolAdaptersContext.Provider>
  )
}
