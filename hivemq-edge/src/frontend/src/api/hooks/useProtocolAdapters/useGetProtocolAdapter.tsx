import { Adapter } from '@/api/__generated__'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'
import { QUERY_KEYS } from '@/api/utils.ts'
import { useQuery } from '@tanstack/react-query'

export const useGetProtocolAdapter = (id: string) => {
  const appClient = useHttpClient()

  return useQuery<Adapter>([QUERY_KEYS.ADAPTERS, id], async () => {
    const adapter = await appClient.protocolAdapters.getAdapter(id)
    return adapter
  })
}
