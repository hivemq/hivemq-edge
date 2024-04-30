import { useQuery } from '@tanstack/react-query'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'
import { QUERY_KEYS } from '@/api/utils.ts'

export const useGetBridge = (name: string | undefined) => {
  const appClient = useHttpClient()

  return useQuery({
    queryKey: [QUERY_KEYS.BRIDGES, name],
    queryFn: () => appClient.bridges.getBridgeByName(name as string),
    enabled: name !== undefined,
  })
}
