import { useQuery } from '@tanstack/react-query'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'
import { ApiError, ProtocolAdaptersList } from '@/api/__generated__'
import { QUERY_KEYS } from '@/api/utils.ts'

export const useGetAdapterTypes = () => {
  const appClient = useHttpClient()

  return useQuery<ProtocolAdaptersList, ApiError>({
    queryKey: [QUERY_KEYS.PROTOCOLS],
    queryFn: async () => {
      const adapterTypes = await appClient.protocolAdapters.getAdapterTypes()
      return adapterTypes
    },
  })
}
