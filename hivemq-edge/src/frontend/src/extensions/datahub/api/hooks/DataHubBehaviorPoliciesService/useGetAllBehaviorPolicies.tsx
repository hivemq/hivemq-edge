import { useQuery } from '@tanstack/react-query'
import { DATAHUB_QUERY_KEYS } from '../../utils.ts'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'
import type { ApiError, BehaviorPolicyList } from '@/api/__generated__'

interface GetAllBehaviorPoliciesProps {
  fields?: string
  policyIds?: string
  clientIds?: string
  limit?: number
  cursor?: string
}

export const useGetAllBehaviorPolicies = ({
  fields,
  policyIds,
  clientIds,
  limit,
  cursor,
}: GetAllBehaviorPoliciesProps) => {
  const appClient = useHttpClient()
  return useQuery<BehaviorPolicyList, ApiError>({
    queryKey: [DATAHUB_QUERY_KEYS.BEHAVIOR_POLICIES, fields, policyIds, clientIds, limit, cursor],
    queryFn: () =>
      appClient.dataHubBehaviorPolicies.getAllBehaviorPolicies(fields, policyIds, clientIds, limit, cursor),
  })
}
