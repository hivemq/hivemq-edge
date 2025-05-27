import { useQuery } from '@tanstack/react-query'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'
import type { Adapter } from '@/api/__generated__'
import { QUERY_KEYS } from '@/api/utils.ts'

export const useGetProtocolAdapter = (id: string) => {
  const appClient = useHttpClient()

  return useQuery<Adapter>({
    queryKey: [QUERY_KEYS.ADAPTERS, id],
    queryFn: () => appClient.protocolAdapters.getAdapter(id),
  })
}
