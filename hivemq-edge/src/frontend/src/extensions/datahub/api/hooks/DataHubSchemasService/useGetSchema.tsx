import { useQuery } from '@tanstack/react-query'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'
import { DATAHUB_QUERY_KEYS } from '../../utils.ts'

export const useGetSchema = (schemaId: string, fields?: string) => {
  const appClient = useHttpClient()
  return useQuery({
    queryKey: [DATAHUB_QUERY_KEYS.SCHEMAS, schemaId, fields],
    queryFn: async () => {
      return appClient.dataHubSchemas.getSchema(schemaId, fields)
    },
  })
}
