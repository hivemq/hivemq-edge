import type { ProtocolAdapter as ApiProtocolAdapter } from '@/api/__generated__'
import { useGetAdapterTypes } from '@/api/hooks/useProtocolAdapters/useGetAdapterTypes'
import { createContext, type FC, type ReactNode, useContext, useMemo } from 'react'

interface ProtocolAdaptersContextValue {
  protocolAdapters: ApiProtocolAdapter[] | undefined
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
    return protocolAdapterTypes?.items || []
  }, [protocolAdapterTypes])

  // Memoize the context value to prevent infinite re-render loop
  const contextValue = useMemo(
    () => ({
      protocolAdapters: protocolAdaptersList,
    }),
    [protocolAdaptersList]
  )

  return <ProtocolAdaptersContext.Provider value={contextValue}>{children}</ProtocolAdaptersContext.Provider>
}
