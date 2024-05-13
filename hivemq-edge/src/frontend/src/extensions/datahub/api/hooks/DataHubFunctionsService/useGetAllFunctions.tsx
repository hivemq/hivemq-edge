import { useQuery } from '@tanstack/react-query'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'
import { ApiError, type JsonNode } from '@/api/__generated__'
import { DATAHUB_QUERY_KEYS } from '@datahub/api/utils.ts'

export const useGetAllFunctions = () => {
  const appClient = useHttpClient()
  return useQuery<JsonNode, ApiError>({
    queryKey: [DATAHUB_QUERY_KEYS.FUNCTIONS],
    queryFn: () => appClient.dataHubFunctions.getFunctions(),
  })
}
