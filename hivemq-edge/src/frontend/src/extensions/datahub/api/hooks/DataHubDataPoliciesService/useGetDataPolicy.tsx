import { useQuery } from '@tanstack/react-query'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'
import { DATAHUB_QUERY_KEYS } from '../../utils.ts'

export const useGetDataPolicy = (policyId: string, fields?: string) => {
  const appClient = useHttpClient()
  return useQuery({
    queryKey: [DATAHUB_QUERY_KEYS.DATA_POLICIES, policyId, fields],
    queryFn: async () => {
      return appClient.dataHubDataPolicies.getDataPolicy(policyId, fields)
    },
  })
}
