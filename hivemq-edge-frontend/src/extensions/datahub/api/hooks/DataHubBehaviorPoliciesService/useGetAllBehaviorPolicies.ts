import { DATAHUB_QUERY_KEYS } from '../../utils.ts'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'
import type { BehaviorPolicy } from '@/api/__generated__'
import { usePaginatedList } from '@datahub/api/hooks/usePaginatedList.ts'

interface GetAllBehaviorPoliciesProps {
  fields?: string
  policyIds?: string
  clientIds?: string
}

export const useGetAllBehaviorPolicies = ({ fields, policyIds, clientIds }: GetAllBehaviorPoliciesProps) => {
  const appClient = useHttpClient()
  // The listing is cursor-paginated; fetch every page so items beyond the first page
  // (default size 50) are not hidden from the UI. See EDG-844.
  return usePaginatedList<BehaviorPolicy>(
    [DATAHUB_QUERY_KEYS.BEHAVIOR_POLICIES, fields, policyIds, clientIds],
    (cursor) =>
      appClient.dataHubBehaviorPolicies.getAllBehaviorPolicies(fields, policyIds, clientIds, undefined, cursor)
  )
}
