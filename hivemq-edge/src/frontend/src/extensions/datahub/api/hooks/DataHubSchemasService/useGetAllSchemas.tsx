import { useQuery } from '@tanstack/react-query'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'
import { DATAHUB_QUERY_KEYS } from '../../utils.ts'

export const useGetAllSchemas = () => {
  const appClient = useHttpClient()
  return useQuery({
    queryKey: [DATAHUB_QUERY_KEYS.SCHEMAS],
    queryFn: async () => {
      return appClient.dataHubSchemas.getAllSchemas()
    },
  })
}
