import { useQuery } from '@tanstack/react-query'
import { ApiError, CapabilityList } from '@/api/__generated__'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'
import { QUERY_KEYS } from '@/api/utils.ts'

export const useGetCapabilities = () => {
  const appClient = useHttpClient()

  return useQuery<CapabilityList, ApiError>({
    queryKey: [QUERY_KEYS.FRONTEND_CAPABILITIES],
    queryFn: () => appClient.frontend.getCapabilities(),
  })
}
