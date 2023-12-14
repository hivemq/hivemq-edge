import { Adapter, ApiError } from '@/api/__generated__'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'
import { QUERY_KEYS } from '@/api/utils.ts'
import { useQuery } from '@tanstack/react-query'

export const useListProtocolAdapters = () => {
  const appClient = useHttpClient()

  return useQuery<Adapter[] | undefined, ApiError>([QUERY_KEYS.ADAPTERS], async () => {
    const { items } = await appClient.protocolAdapters.getAdapters()
    return items
  })
}
