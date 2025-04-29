import { useQuery } from '@tanstack/react-query'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'
import { DATAHUB_QUERY_KEYS } from '../../utils.ts'
import type { ApiError, Script } from '@/api/__generated__'

export const useGetScript = (scriptId: string, fields?: string) => {
  const appClient = useHttpClient()
  return useQuery<Script, ApiError>({
    queryKey: [DATAHUB_QUERY_KEYS.SCRIPTS, scriptId, fields],
    queryFn: () => appClient.dataHubScripts.getScript(scriptId, fields),
  })
}
