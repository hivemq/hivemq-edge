import { useQuery } from '@tanstack/react-query'
import { DATAHUB_QUERY_KEYS } from '../../utils.ts'
import { useHttpClient } from '../useHttpClient/useHttpClient.ts'

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
  return useQuery({
    queryKey: [DATAHUB_QUERY_KEYS.BEHAVIOR_POLICIES],
    queryFn: async () => {
      return appClient.dataHubBehaviorPolicies.getAllBehaviorPolicies(fields, policyIds, clientIds, limit, cursor)
    },
  })
}
