import { useQuery } from '@tanstack/react-query'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'
import { ApiError, Bridge } from '@/api/__generated__'
import { QUERY_KEYS } from '@/api/utils.ts'

export const useListBridges = () => {
  const appClient = useHttpClient()

  return useQuery<Bridge[] | undefined, ApiError>([QUERY_KEYS.BRIDGES], async () => {
    const { items } = await appClient.bridges.getBridges()
    return items
  })
}
