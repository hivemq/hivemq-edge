import { useQuery } from '@tanstack/react-query'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'
import { DATAHUB_QUERY_KEYS } from '../../utils.ts'
import type { ApiError, DataPolicy } from '@/api/__generated__'

export const useGetDataPolicy = (policyId: string, fields?: string) => {
  const appClient = useHttpClient()
  return useQuery<DataPolicy, ApiError>({
    queryKey: [DATAHUB_QUERY_KEYS.DATA_POLICIES, policyId, fields],
    queryFn: () => appClient.dataHubDataPolicies.getDataPolicy(policyId, fields),
  })
}
