import { useQuery } from '@tanstack/react-query'
import { DATAHUB_QUERY_KEYS } from '../../utils.ts'
import { useHttpClient } from '../useHttpClient/useHttpClient.ts'

export const useGetAllSchemas = () => {
  const appClient = useHttpClient()
  return useQuery({
    queryKey: [DATAHUB_QUERY_KEYS.SCHEMAS],
    queryFn: async () => {
      return appClient.dataHubSchemas.getAllSchemas()
    },
  })
}
