import { useQuery } from '@tanstack/react-query'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'
import { DATAHUB_QUERY_KEYS } from '../../utils.ts'

export const useGetScript = (scriptId: string, fields?: string) => {
  const appClient = useHttpClient()
  return useQuery({
    queryKey: [DATAHUB_QUERY_KEYS.SCRIPTS, scriptId, fields],
    queryFn: async () => {
      return appClient.dataHubScripts.getScript(scriptId, fields)
    },
  })
}
