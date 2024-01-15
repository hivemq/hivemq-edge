import { useQuery } from '@tanstack/react-query'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'
import { DATAHUB_QUERY_KEYS } from '../../utils.ts'

interface GetAllScriptsProps {
  fields?: string
  functionTypes?: string
  scriptIds?: string
  limit?: number
  cursor?: string
}

export const useGetAllScripts = ({ fields, functionTypes, scriptIds, limit, cursor }: GetAllScriptsProps) => {
  const appClient = useHttpClient()
  return useQuery({
    queryKey: [DATAHUB_QUERY_KEYS.SCRIPTS],
    queryFn: async () => {
      return appClient.dataHubScripts.getAllScripts(fields, functionTypes, scriptIds, limit, cursor)
    },
  })
}
