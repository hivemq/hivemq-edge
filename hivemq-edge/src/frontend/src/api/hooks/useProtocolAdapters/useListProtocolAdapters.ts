import { useQuery } from '@tanstack/react-query'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'
import type { Adapter, ApiError } from '@/api/__generated__'
import { QUERY_KEYS } from '@/api/utils.ts'

export const useListProtocolAdapters = () => {
  const appClient = useHttpClient()

  return useQuery<Adapter[] | undefined, ApiError>({
    queryKey: [QUERY_KEYS.ADAPTERS],
    queryFn: async () => {
      const { items } = await appClient.protocolAdapters.getAdapters()
      return items
    },
  })
}
