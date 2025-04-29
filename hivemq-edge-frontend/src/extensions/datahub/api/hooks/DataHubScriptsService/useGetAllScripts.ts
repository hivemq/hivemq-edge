import { useQuery } from '@tanstack/react-query'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'
import type { ApiError, ScriptList } from '@/api/__generated__'
import { DATAHUB_QUERY_KEYS } from '@datahub/api/utils.ts'

interface GetAllScriptsProps {
  fields?: string
  functionTypes?: string
  scriptIds?: string
  limit?: number
  cursor?: string
}

export const useGetAllScripts = ({ fields, functionTypes, scriptIds, limit, cursor }: GetAllScriptsProps) => {
  const appClient = useHttpClient()
  return useQuery<ScriptList, ApiError>({
    queryKey: [DATAHUB_QUERY_KEYS.SCRIPTS, fields, functionTypes, scriptIds, limit, cursor],
    queryFn: () => appClient.dataHubScripts.getAllScripts(fields, functionTypes, scriptIds, limit, cursor),
  })
}
