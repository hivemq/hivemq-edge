import { useQuery } from '@tanstack/react-query'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'
import { Adapter } from '@/api/__generated__'
import { QUERY_KEYS } from '@/api/utils.ts'

export const useGetProtocolAdapter = (id: string) => {
  const appClient = useHttpClient()

  return useQuery<Adapter>([QUERY_KEYS.ADAPTERS, id], async () => {
    const adapter = await appClient.protocolAdapters.getAdapter(id)
    return adapter
  })
}
