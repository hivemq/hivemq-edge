import { useQuery } from '@tanstack/react-query'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'
import type { ApiError, SchemaList } from '@/api/__generated__'
import { DATAHUB_QUERY_KEYS } from '@datahub/api/utils.ts'

export const useGetAllSchemas = () => {
  const appClient = useHttpClient()
  return useQuery<SchemaList, ApiError>({
    queryKey: [DATAHUB_QUERY_KEYS.SCHEMAS],
    queryFn: () => appClient.dataHubSchemas.getAllSchemas(),
  })
}
