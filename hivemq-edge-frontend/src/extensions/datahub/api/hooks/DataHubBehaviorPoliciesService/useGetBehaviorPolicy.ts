import { useQuery } from '@tanstack/react-query'
import { DATAHUB_QUERY_KEYS } from '../../utils.ts'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'
import type { ApiError, BehaviorPolicy } from '@/api/__generated__'

export const useGetBehaviorPolicy = (policyId: string, fields?: string) => {
  const appClient = useHttpClient()
  return useQuery<BehaviorPolicy, ApiError>({
    queryKey: [DATAHUB_QUERY_KEYS.BEHAVIOR_POLICIES, policyId, fields],
    queryFn: () => appClient.dataHubBehaviorPolicies.getBehaviorPolicy(policyId, fields),
  })
}
