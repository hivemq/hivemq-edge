import { useQuery } from '@tanstack/react-query'
import { DATAHUB_QUERY_KEYS } from '../../utils.ts'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'

export const useGetBehaviorPolicy = (policyId: string, fields?: string) => {
  const appClient = useHttpClient()
  return useQuery({
    queryKey: [DATAHUB_QUERY_KEYS.BEHAVIOR_POLICIES, policyId, fields],
    queryFn: async () => {
      return appClient.dataHubBehaviorPolicies.getBehaviorPolicy(policyId, fields)
    },
  })
}
