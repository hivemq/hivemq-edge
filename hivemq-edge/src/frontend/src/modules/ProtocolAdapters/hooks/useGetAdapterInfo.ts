import { useMemo } from 'react'
import { ProtocolAdapter } from '@/api/__generated__'
import { useGetAdapterTypes } from '@/api/hooks/useProtocolAdapters/useGetAdapterTypes.ts'
import { useListProtocolAdapters } from '@/api/hooks/useProtocolAdapters/useListProtocolAdapters.ts'

const useGetAdapterInfo = (adapterId: string | undefined) => {
  const { data: allProtocols, isLoading: isProtocolLoading } = useGetAdapterTypes()
  const { data: allAdapters, isLoading: isAdapterLoading } = useListProtocolAdapters()

  const { adapter, isDiscoverable, name, logo, configSchema, uiSchema } = useMemo(() => {
    const adapter = allAdapters?.find((e) => e.id === adapterId)
    const protocol: ProtocolAdapter | undefined = allProtocols?.items?.find((e) => e.id === adapter?.type)
    const { capabilities, configSchema, uiSchema } = protocol || {}

    return {
      isDiscoverable: capabilities?.includes('DISCOVER'),
      name: protocol?.name,
      logo: protocol?.logoUrl,
      adapter: adapter,
      configSchema,
      uiSchema,
    }
  }, [adapterId, allAdapters, allProtocols])

  const isLoading = isAdapterLoading || isProtocolLoading

  return { isLoading, isDiscoverable, name, logo, adapter, configSchema, uiSchema }
}

export default useGetAdapterInfo
