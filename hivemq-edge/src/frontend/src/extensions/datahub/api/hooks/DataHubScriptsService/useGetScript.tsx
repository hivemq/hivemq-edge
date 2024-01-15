import { useQuery } from '@tanstack/react-query'
import { DATAHUB_QUERY_KEYS } from '../../utils.ts'
import { useHttpClient } from '../useHttpClient/useHttpClient.ts'

export const useGetScript = (scriptId: string, fields?: string) => {
  const appClient = useHttpClient()
  return useQuery({
    queryKey: [DATAHUB_QUERY_KEYS.SCRIPTS, scriptId, fields],
    queryFn: async () => {
      return appClient.dataHubScripts.getScript(scriptId, fields)
    },
  })
}
