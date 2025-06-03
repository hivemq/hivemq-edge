import { useQuery } from '@tanstack/react-query'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'
import type { ApiError } from '@/api/__generated__'
import { type JsonNode } from '@/api/__generated__'
import { DATAHUB_QUERY_KEYS } from '@datahub/api/utils.ts'

/**
 * @deprecated This hook is deprecated and will be removed in the future.
 *  Please use `useGetAllFunctionSpecs` instead.
 * @see useGetAllFunctionSpecs
 */
export const useGetAllFunctions = () => {
  const appClient = useHttpClient()
  return useQuery<JsonNode, ApiError>({
    queryKey: [DATAHUB_QUERY_KEYS.FUNCTIONS],
    queryFn: () => appClient.dataHubFunctions.getFunctions(),
  })
}
