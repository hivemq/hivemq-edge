import { useMemo } from 'react'
import type { ProtocolAdapter } from '@/api/__generated__'
import { useGetAdapterTypes } from '@/api/hooks/useProtocolAdapters/useGetAdapterTypes.ts'
import { useListProtocolAdapters } from '@/api/hooks/useProtocolAdapters/useListProtocolAdapters.ts'

const useGetAdapterInfo = (adapterId: string | undefined) => {
  const { data: allProtocols, isLoading: isProtocolLoading } = useGetAdapterTypes()
  const { data: allAdapters, isLoading: isAdapterLoading } = useListProtocolAdapters()

  const { adapter, isDiscoverable, protocol } = useMemo(() => {
    const adapter = allAdapters?.find((adapter) => adapter.id === adapterId)
    const protocol: ProtocolAdapter | undefined = allProtocols?.items?.find(
      (protocolAdapter) => protocolAdapter.id === adapter?.type
    )
    const { capabilities } = protocol || {}

    return {
      isDiscoverable: capabilities?.includes('DISCOVER'),
      adapter,
      protocol,
    }
  }, [adapterId, allAdapters, allProtocols])

  const isLoading = isAdapterLoading || isProtocolLoading

  return { isLoading, isDiscoverable, adapter, protocol }
}

export default useGetAdapterInfo
