import { useQuery } from '@tanstack/react-query'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'
import type { ApiError, FunctionSpecsList } from '@/api/__generated__'
import { DATAHUB_QUERY_KEYS } from '@datahub/api/utils.ts'

export const useGetAllFunctionSpecs = () => {
  const appClient = useHttpClient()
  return useQuery<FunctionSpecsList, ApiError>({
    queryKey: [DATAHUB_QUERY_KEYS.FUNCTIONS],
    queryFn: () => appClient.dataHubFunctions.getFunctionSpecs(),
  })
}
