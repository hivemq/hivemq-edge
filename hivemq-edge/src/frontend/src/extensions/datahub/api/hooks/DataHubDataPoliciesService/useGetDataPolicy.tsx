import { useQuery } from '@tanstack/react-query'
import { DATAHUB_QUERY_KEYS } from '../../utils.ts'
import { useHttpClient } from '../useHttpClient/useHttpClient.ts'

export const useGetDataPolicy = (policyId: string, fields?: string) => {
  const appClient = useHttpClient()
  return useQuery({
    queryKey: [DATAHUB_QUERY_KEYS.DATA_POLICIES, policyId, fields],
    queryFn: async () => {
      return appClient.dataHubDataPolicies.getDataPolicy(policyId, fields)
    },
  })
}
