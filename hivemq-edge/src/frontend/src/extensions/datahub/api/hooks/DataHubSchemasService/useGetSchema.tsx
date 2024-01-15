import { useQuery } from '@tanstack/react-query'
import { DATAHUB_QUERY_KEYS } from '../../utils.ts'
import { useHttpClient } from '../useHttpClient/useHttpClient.ts'

export const useGetSchema = (schemaId: string, fields?: string) => {
  const appClient = useHttpClient()
  return useQuery({
    queryKey: [DATAHUB_QUERY_KEYS.SCHEMAS, schemaId, fields],
    queryFn: async () => {
      return appClient.dataHubSchemas.getSchema(schemaId, fields)
    },
  })
}
