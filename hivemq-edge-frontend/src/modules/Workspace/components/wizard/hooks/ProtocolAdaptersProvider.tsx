import { useGetAdapterTypes } from '@/api/hooks/useProtocolAdapters/useGetAdapterTypes.ts'
import { ProtocolAdaptersContext } from '@/modules/Workspace/components/wizard/hooks/ProtocolAdaptersContext.tsx'
import { type FC, type ReactNode, useMemo } from 'react'

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
