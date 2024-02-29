import { useQuery } from '@tanstack/react-query'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'
import { QUERY_KEYS } from '@/api/utils.ts'
import { ApiError, CapabilityList } from '@/api/__generated__'

export const useGetCapabilities = () => {
  const appClient = useHttpClient()

  return useQuery<CapabilityList, ApiError>([QUERY_KEYS.FRONTEND_CAPABILITIES], async () => {
    const item = await appClient.frontend.getCapabilities()
    return item
  })
}
