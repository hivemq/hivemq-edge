import { useMemo } from 'react'
import { ProtocolAdapter } from '@/api/__generated__'
import { useGetAdapterTypes } from '@/api/hooks/useProtocolAdapters/useGetAdapterTypes.ts'
import { useListProtocolAdapters } from '@/api/hooks/useProtocolAdapters/useListProtocolAdapters.ts'

const useGetAdapterInfo = (adapterId: string | undefined) => {
  const { data: allProtocols, isLoading: isProtocolLoading } = useGetAdapterTypes()
  const { data: allAdapters, isLoading: isAdapterLoading } = useListProtocolAdapters()

  const { isDiscoverable, schema, name, logo } = useMemo(() => {
    const { type } = allAdapters?.find((e) => e.id === adapterId) || {}
    const adapter: ProtocolAdapter | undefined = allProtocols?.items?.find((e) => e.id === type)
    const { capabilities, configSchema } = adapter || {}

    return {
      isDiscoverable: Boolean(capabilities?.includes('DISCOVER')),
      schema: configSchema,
      name: adapter?.name,
      logo: adapter?.logoUrl,
    }
  }, [adapterId, allAdapters, allProtocols])

  const isLoading = isAdapterLoading || isProtocolLoading

  return { isLoading, isDiscoverable, schema, name, logo }
}

export default useGetAdapterInfo
