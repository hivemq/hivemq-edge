import { useQuery } from '@tanstack/react-query'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'
import type { ApiError, Bridge } from '@/api/__generated__'
import { QUERY_KEYS } from '@/api/utils.ts'

export const useListBridges = () => {
  const appClient = useHttpClient()

  return useQuery<Bridge[] | undefined, ApiError>({
    queryKey: [QUERY_KEYS.BRIDGES],
    queryFn: async () => {
      const { items } = await appClient.bridges.getBridges()
      return items
    },
  })
}
