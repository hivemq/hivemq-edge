import { ApiError, Bridge } from '@/api/__generated__'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'
import { QUERY_KEYS } from '@/api/utils.ts'
import { useQuery } from '@tanstack/react-query'

export const useListBridges = () => {
  const appClient = useHttpClient()

  return useQuery<Bridge[] | undefined, ApiError>([QUERY_KEYS.BRIDGES], async () => {
    const { items } = await appClient.bridges.getBridges()
    return items
  })
}
