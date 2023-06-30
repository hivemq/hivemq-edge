import { useQuery } from '@tanstack/react-query'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'
import { QUERY_KEYS } from '@/api/utils.ts'

export const useGetBridge = (name: string | undefined) => {
  const appClient = useHttpClient()

  return useQuery(
    [QUERY_KEYS.BRIDGES, name],
    async () => {
      const item = await appClient.bridges.getBridgeByName(name as string)
      return item
    },
    { enabled: name !== undefined }
  )
}
