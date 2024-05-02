import { useQuery } from '@tanstack/react-query'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'
import { DATAHUB_QUERY_KEYS } from '../../utils.ts'
import { ApiError, Schema } from '@/api/__generated__'

export const useGetSchema = (schemaId: string, fields?: string) => {
  const appClient = useHttpClient()
  return useQuery<Schema, ApiError>({
    queryKey: [DATAHUB_QUERY_KEYS.SCHEMAS, schemaId, fields],
    queryFn: () => appClient.dataHubSchemas.getSchema(schemaId, fields),
  })
}
